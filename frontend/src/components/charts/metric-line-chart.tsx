import { useMemo, useState } from 'react';
import { LayoutChangeEvent, ScrollView, View } from 'react-native';
import Svg, { Circle, G, Line, Path, Text as SvgText } from 'react-native-svg';

import { AppText } from '@/components/ui/text';
import { ChartPoint } from '@/lib/api';
import { formatTick } from '@/lib/dates';
import { getMetricStats } from '@/lib/status';
import { formatMetric } from '@/lib/utils';

type MetricLineChartProps = {
  color: string;
  points: ChartPoint[];
  title: string;
  unit: string;
  valueKey: 'temperature' | 'humidity';
};

const chartHeight = 170;
const chartPadding = {
  top: 18,
  right: 18,
  bottom: 34,
  left: 42,
};

export function MetricLineChart({
  color,
  points,
  title,
  unit,
  valueKey,
}: MetricLineChartProps) {
  const [viewportWidth, setViewportWidth] = useState(0);

  const values = useMemo(
    () =>
      points
        .map((point) => ({
          timestamp: point.timestamp,
          value: point[valueKey],
        }))
        .filter((point): point is { timestamp: string; value: number } =>
          typeof point.value === 'number' && Number.isFinite(point.value),
        ),
    [points, valueKey],
  );

  const latest = values.at(-1)?.value;
  const stats = getMetricStats(points, valueKey);
  const chartWidth = Math.max(viewportWidth || 320, values.length * 30, 320);
  const plotWidth = chartWidth - chartPadding.left - chartPadding.right;
  const plotHeight = chartHeight - chartPadding.top - chartPadding.bottom;
  const minValue = values.length ? Math.min(...values.map((point) => point.value)) : 0;
  const maxValue = values.length ? Math.max(...values.map((point) => point.value)) : 0;
  const range = maxValue - minValue || 1;
  const paddedMin = minValue - range * 0.12;
  const paddedMax = maxValue + range * 0.12;
  const paddedRange = paddedMax - paddedMin || 1;

  const coordinates = values.map((point, index) => {
    const x =
      chartPadding.left +
      (values.length === 1 ? plotWidth / 2 : (index / (values.length - 1)) * plotWidth);
    const y =
      chartPadding.top + ((paddedMax - point.value) / paddedRange) * plotHeight;

    return { ...point, x, y };
  });

  const path = coordinates
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`)
    .join(' ');

  function onLayout(event: LayoutChangeEvent) {
    setViewportWidth(event.nativeEvent.layout.width);
  }

  return (
    <View className="gap-3 rounded-card border border-dashboard-border bg-dashboard-surface p-4">
      <View className="flex-row items-start justify-between gap-3">
        <View>
          <AppText variant="label">{title}</AppText>
          <AppText variant="metric">
            {formatMetric(latest)}
            <AppText className="text-xl text-dashboard-muted"> {unit}</AppText>
          </AppText>
        </View>
        {stats ? (
          <View className="items-end">
            <AppText variant="caption">Avg {formatMetric(stats.average)} {unit}</AppText>
            <AppText variant="caption">High {formatMetric(stats.high)} {unit}</AppText>
            <AppText variant="caption">Low {formatMetric(stats.low)} {unit}</AppText>
          </View>
        ) : null}
      </View>

      {values.length ? (
        <View onLayout={onLayout}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <Svg height={chartHeight} width={chartWidth}>
              {[0, 0.5, 1].map((ratio) => {
                const y = chartPadding.top + ratio * plotHeight;
                const value = paddedMax - ratio * paddedRange;

                return (
                  <G key={ratio}>
                    <Line
                      stroke="#E5E7EB"
                      strokeDasharray="4 6"
                      strokeWidth={1}
                      x1={chartPadding.left}
                      x2={chartWidth - chartPadding.right}
                      y1={y}
                      y2={y}
                    />
                    <SvgText
                      fill="#667085"
                      fontSize={11}
                      textAnchor="end"
                      x={chartPadding.left - 8}
                      y={y + 4}>
                      {formatMetric(value)}
                    </SvgText>
                  </G>
                );
              })}

              <Path d={path} fill="none" stroke={color} strokeLinecap="round" strokeWidth={3} />

              {coordinates.map((point, index) => {
                const isMajorPoint =
                  index === 0 || index === coordinates.length - 1 || values.length <= 12;

                return (
                  <Circle
                    cx={point.x}
                    cy={point.y}
                    fill={isMajorPoint ? color : '#FFFFFF'}
                    key={`${point.timestamp}-${index}`}
                    r={isMajorPoint ? 3.5 : 2}
                    stroke={color}
                    strokeWidth={2}
                  />
                );
              })}

              {coordinates.at(0) ? (
                <SvgText
                  fill="#667085"
                  fontSize={11}
                  textAnchor="start"
                  x={chartPadding.left}
                  y={chartHeight - 8}>
                  {formatTick(coordinates[0].timestamp)}
                </SvgText>
              ) : null}
              {coordinates.at(-1) ? (
                <SvgText
                  fill="#667085"
                  fontSize={11}
                  textAnchor="end"
                  x={chartWidth - chartPadding.right}
                  y={chartHeight - 8}>
                  {formatTick(coordinates[coordinates.length - 1].timestamp)}
                </SvgText>
              ) : null}
            </Svg>
          </ScrollView>
        </View>
      ) : (
        <View className="h-36 items-center justify-center rounded-card bg-dashboard-soft px-4">
          <AppText className="text-center" variant="caption">
            No chart points yet.
          </AppText>
        </View>
      )}
    </View>
  );
}
