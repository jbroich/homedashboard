const dayFormatter = new Intl.DateTimeFormat(undefined, {
  weekday: 'short',
  month: 'short',
  day: 'numeric',
});

const dateTimeFormatter = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const monthFormatter = new Intl.DateTimeFormat(undefined, {
  month: 'long',
  year: 'numeric',
});

const tickFormatter = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
});

const pad = (value: number) => String(value).padStart(2, '0');

export function formatDayLabel(date: Date) {
  return dayFormatter.format(date);
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'No update';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return 'No update';
  }

  return dateTimeFormatter.format(date);
}

export function formatTick(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return tickFormatter.format(date);
}

export function formatMonthTitle(date: Date) {
  return monthFormatter.format(date);
}

export function dateKey(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function addDays(date: Date, days: number) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

export function addMonths(date: Date, months: number) {
  return new Date(date.getFullYear(), date.getMonth() + months, 1);
}

export function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

export function isSameDay(a: Date, b: Date) {
  return dateKey(a) === dateKey(b);
}

export function isAfterDay(a: Date, b: Date) {
  return new Date(a.getFullYear(), a.getMonth(), a.getDate()).getTime() >
    new Date(b.getFullYear(), b.getMonth(), b.getDate()).getTime();
}

export function toEndOfDayOffsetDateTime(date: Date) {
  const localDate = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59);
  const offsetMinutes = -localDate.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? '+' : '-';
  const absoluteOffset = Math.abs(offsetMinutes);

  return [
    `${localDate.getFullYear()}-${pad(localDate.getMonth() + 1)}-${pad(localDate.getDate())}`,
    `T23:59:59${sign}${pad(Math.floor(absoluteOffset / 60))}:${pad(absoluteOffset % 60)}`,
  ].join('');
}

export type CalendarCell = {
  key: string;
  date: Date | null;
};

export function getCalendarCells(monthDate: Date): CalendarCell[] {
  const first = startOfMonth(monthDate);
  const daysInMonth = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  const mondayFirstOffset = (first.getDay() + 6) % 7;
  const cells: CalendarCell[] = [];

  for (let index = 0; index < mondayFirstOffset; index += 1) {
    cells.push({ key: `blank-start-${index}`, date: null });
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = new Date(first.getFullYear(), first.getMonth(), day);
    cells.push({ key: dateKey(date), date });
  }

  while (cells.length % 7 !== 0) {
    cells.push({ key: `blank-end-${cells.length}`, date: null });
  }

  return cells;
}
