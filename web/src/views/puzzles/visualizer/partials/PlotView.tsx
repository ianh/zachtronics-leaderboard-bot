/*
 * Copyright (c) 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useTheme } from "@mui/material"
import { ComponentType, useState } from "react"
import OmRecord, { isStrictlyBetterInMetrics } from "../../../../model/Record"
import { Axis, PlotData } from "plotly.js"
import { PlotParams } from "react-plotly.js"
import createPlotlyComponent from "react-plotly.js/factory"
import Plotly from "plotly.js-gl3d-dist-min"
import Metric, { getMetric } from "../../../../model/Metric"
import { Configuration } from "../../../../model/Configuration"
import { RecordModal } from "../../../../fragments/RecordModal"
import { withSize } from "react-sizeme"
import { applyFilter, Filter } from "../../../../model/Filter"

export interface PlotViewProps {
    size: { width: number; height: number }
    records: OmRecord[]
    configuration: Configuration
    filter: Filter
}

const Plot: ComponentType<PlotParams> = createPlotlyComponent(Plotly)

function getColor(record: OmRecord) {
    return record.score?.overlap ? "#880e4f" : record.score?.trackless ? "#558b2f" : "#0288d1"
}

function PlotView(props: PlotViewProps) {
    const [activeRecord, setActiveRecord] = useState<OmRecord | undefined>(undefined)

    const configuration = props.configuration
    const records = applyFilter(props.filter, configuration, props.records)
    const x = getMetric(configuration.x)
    const y = getMetric(configuration.y)
    const z = getMetric(configuration.z)

    const theme = useTheme()
    const gridColor = theme.palette.mode === "light" ? theme.palette.grey["200"] : theme.palette.grey["800"]
    const makeAxis = (metric: Metric): Partial<Axis> => {
        return {
            title: metric.name,
            color: theme.palette.text.primary,
            gridcolor: gridColor,
            zerolinecolor: gridColor,
            rangemode: "tozero",
        }
    }

    const common: Partial<PlotData> = {
        hovertext: records.map((record: OmRecord) => `${record.fullFormattedScore ?? "none"}<br>${record.smartFormattedCategories}`),
        x: records.map((record) => x.get(record) ?? 0),
        y: records.map((record) => y.get(record) ?? 0),
        mode: "markers",
        marker: {
            line: {
                color: records.map(getColor),
            },
        },
        ids: records.map((record) => record.fullFormattedScore ?? "none"),
    }

    const getMarkerColors = (...metrics: Metric[]) => records.map((record: OmRecord) => (records.some((r) => isStrictlyBetterInMetrics(r, record, metrics)) ? "#00000000" : getColor(record)))

    return (
        <>
            <Plot
                data={[
                    configuration.mode === "2D"
                        ? {
                              ...common,
                              hovertemplate: `${x.name}: %{x}<br>${y.name}: %{y}<br>%{hovertext}<extra></extra>`,
                              type: "scatter",
                              marker: {
                                  ...common.marker,
                                  size: 20,
                                  color: getMarkerColors(x, y),
                                  line: {
                                      ...common.marker?.line,
                                      width: 3,
                                  },
                              },
                          }
                        : {
                              ...common,
                              z: records.map((record) => z.get(record) ?? 0),
                              hovertemplate: `${x.name}: %{x}<br>${y.name}: %{y}<br>${z.name}: %{z}<br>%{hovertext}<extra></extra>`,
                              type: "scatter3d",
                              marker: {
                                  ...common.marker,
                                  size: 10,
                                  color: getMarkerColors(x, y, z),
                                  line: {
                                      ...common.marker?.line,
                                      width: 2,
                                  },
                              },
                          },
                ]}
                layout={{
                    autosize: false,
                    width: props.size.width ?? undefined,
                    height: props.size.height ?? undefined,
                    paper_bgcolor: "transparent",
                    plot_bgcolor: "transparent",
                    xaxis: makeAxis(x),
                    yaxis: makeAxis(y),
                    hoverlabel: {
                        bgcolor: theme.palette.background.paper,
                    },
                    scene: {
                        xaxis: makeAxis(x),
                        yaxis: makeAxis(y),
                        zaxis: makeAxis(z),
                    },
                    transition: {
                        duration: 500,
                        easing: "cubic-in-out",
                    },
                }}
                config={{
                    displaylogo: false,
                }}
                style={{
                    width: "100%",
                    height: "100%",
                    flexGrow: 1,
                }}
                useResizeHandler={true}
                onClick={(event) => {
                    setActiveRecord(records[event.points[0].pointIndex])
                }}
            />
            <RecordModal record={activeRecord} setRecord={setActiveRecord} />
        </>
    )
}

const SizedPuzzleFrontierPlot = withSize({ monitorHeight: true })<PlotViewProps>(PlotView)

export default SizedPuzzleFrontierPlot
