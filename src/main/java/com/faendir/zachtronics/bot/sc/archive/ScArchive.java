package com.faendir.zachtronics.bot.sc.archive;

import com.faendir.zachtronics.bot.generic.archive.Archive;
import com.faendir.zachtronics.bot.main.git.GitRepository;
import com.faendir.zachtronics.bot.sc.model.ScSolution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScArchive implements Archive<ScSolution> {
    @Qualifier("scArchiveRepository")
    private final GitRepository gitRepo;

    @NotNull
    @Override
    public List<String> archive(@NotNull ScSolution solution) {
        return gitRepo.access(a -> performArchive(a, solution));
    }

    @NotNull
    private List<String> performArchive(GitRepository.AccessScope accessScope, @NotNull ScSolution solution) {
        Path repoPath = accessScope.getRepo().toPath();
        Path puzzlePath = repoPath.resolve(solution.getPuzzle().getGroup().name()).resolve(solution.getPuzzle().name());
        boolean needToUpdate;
        try {
            SolutionsIndex index = new SolutionsIndex(puzzlePath);
            needToUpdate = index.add(solution);
        } catch (IOException e) {
            // failures could happen after we dirtied the repo, so we call reset&clean on the puzzle dir
            accessScope.resetAndClean(puzzlePath.toFile());
            log.warn("Recoverable error during archive: ", e);
            return Collections.emptyList();
        }

        if (needToUpdate) {
            accessScope.add(puzzlePath.toFile());
            accessScope.commitAndPush(
                    "Added " + solution.getScore().toDisplayString() + " for " + solution.getPuzzle());
            return Collections.singletonList(solution.getScore().toDisplayString());
        }
        else {
            return Collections.emptyList();
        }
    }
}
