import { Text, TextProps } from 'react-native';

import { cn } from '@/lib/utils';

type AppTextProps = TextProps & {
  variant?: 'title' | 'subtitle' | 'body' | 'caption' | 'metric' | 'label';
};

const variants = {
  title: 'text-3xl font-bold leading-9 text-dashboard-text',
  subtitle: 'text-xl font-semibold leading-7 text-dashboard-text',
  body: 'text-base leading-6 text-dashboard-text',
  caption: 'text-sm leading-5 text-dashboard-muted',
  metric: 'text-3xl font-bold leading-9 text-dashboard-text',
  label: 'text-xs font-semibold uppercase leading-4 text-dashboard-muted',
};

export function AppText({ className, variant = 'body', ...props }: AppTextProps) {
  return <Text className={cn(variants[variant], className)} {...props} />;
}
