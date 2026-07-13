import { View, ViewProps } from 'react-native';

import { cn } from '@/lib/utils';

export function Card({ className, ...props }: ViewProps) {
  return (
    <View
      className={cn('rounded-card border border-dashboard-border bg-dashboard-surface p-4', className)}
      {...props}
    />
  );
}
