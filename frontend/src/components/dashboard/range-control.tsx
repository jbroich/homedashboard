import { Pressable, View } from 'react-native';

import { AppText } from '@/components/ui/text';
import { cn } from '@/lib/utils';

export type ChartRange = 'day' | 'week' | 'month';

const ranges: { label: string; value: ChartRange }[] = [
  { label: 'Day', value: 'day' },
  { label: 'Week', value: 'week' },
  { label: 'Month', value: 'month' },
];

type RangeControlProps = {
  value: ChartRange;
  onChange: (range: ChartRange) => void;
};

export function RangeControl({ onChange, value }: RangeControlProps) {
  return (
    <View className="flex-row rounded-card border border-dashboard-border bg-dashboard-surface p-1">
      {ranges.map((range) => {
        const selected = range.value === value;

        return (
          <Pressable
            accessibilityRole="button"
            accessibilityState={{ selected }}
            className={cn(
              'h-10 flex-1 items-center justify-center rounded-md',
              selected && 'bg-dashboard-primary',
            )}
            key={range.value}
            onPress={() => onChange(range.value)}>
            <AppText
              className={selected ? 'text-white' : 'text-dashboard-muted'}
              variant="caption">
              {range.label}
            </AppText>
          </Pressable>
        );
      })}
    </View>
  );
}
