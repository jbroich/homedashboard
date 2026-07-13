import { ChevronLeft, ChevronRight } from 'lucide-react-native';
import { Pressable, View } from 'react-native';

import { AppText } from '@/components/ui/text';
import {
  addMonths,
  formatDayLabel,
  formatMonthTitle,
  getCalendarCells,
  isAfterDay,
  isSameDay,
  startOfMonth,
} from '@/lib/dates';
import { palette } from '@/lib/theme';
import { cn } from '@/lib/utils';

const weekdays = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

type CalendarPickerProps = {
  selectedDate: Date;
  onSelectDate: (date: Date) => void;
  visibleMonth: Date;
  onVisibleMonthChange: (date: Date) => void;
};

export function CalendarPicker({
  onSelectDate,
  onVisibleMonthChange,
  selectedDate,
  visibleMonth,
}: CalendarPickerProps) {
  const today = new Date();
  const cells = getCalendarCells(visibleMonth);
  const currentMonth = startOfMonth(today);
  const nextMonthDisabled =
    startOfMonth(addMonths(visibleMonth, 1)).getTime() > currentMonth.getTime();

  return (
    <View className="gap-3 rounded-card border border-dashboard-border bg-dashboard-surface p-4">
      <View className="flex-row items-center justify-between">
        <Pressable
          accessibilityLabel="Previous month"
          accessibilityRole="button"
          className="h-10 w-10 items-center justify-center rounded-md bg-dashboard-soft"
          onPress={() => onVisibleMonthChange(addMonths(visibleMonth, -1))}>
          <ChevronLeft color={palette.primary} size={20} />
        </Pressable>

        <View className="items-center">
          <AppText variant="subtitle">{formatMonthTitle(visibleMonth)}</AppText>
          <AppText variant="caption">{formatDayLabel(selectedDate)}</AppText>
        </View>

        <Pressable
          accessibilityLabel="Next month"
          accessibilityRole="button"
          className={cn(
            'h-10 w-10 items-center justify-center rounded-md bg-dashboard-soft',
            nextMonthDisabled && 'opacity-40',
          )}
          disabled={nextMonthDisabled}
          onPress={() => onVisibleMonthChange(addMonths(visibleMonth, 1))}>
          <ChevronRight color={palette.primary} size={20} />
        </Pressable>
      </View>

      <View className="flex-row">
        {weekdays.map((weekday, index) => (
          <View className="h-8 flex-1 items-center justify-center" key={`${weekday}-${index}`}>
            <AppText className="text-dashboard-muted" variant="label">
              {weekday}
            </AppText>
          </View>
        ))}
      </View>

      <View className="flex-row flex-wrap">
        {cells.map((cell) => {
          const selected = cell.date ? isSameDay(cell.date, selectedDate) : false;
          const disabled = cell.date ? isAfterDay(cell.date, today) : true;

          return (
            <View className="h-10 w-[14.285714%] p-0.5" key={cell.key}>
              {cell.date ? (
                <Pressable
                  accessibilityRole="button"
                  accessibilityState={{ disabled, selected }}
                  className={cn(
                    'h-full items-center justify-center rounded-md',
                    selected && 'bg-dashboard-primary',
                    !selected && !disabled && 'bg-dashboard-soft',
                    disabled && 'opacity-30',
                  )}
                  disabled={disabled}
                  onPress={() => onSelectDate(cell.date as Date)}>
                  <AppText
                    className={selected ? 'text-white' : 'text-dashboard-text'}
                    variant="caption">
                    {cell.date.getDate()}
                  </AppText>
                </Pressable>
              ) : null}
            </View>
          );
        })}
      </View>
    </View>
  );
}
