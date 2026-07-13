import { Pressable, PressableProps, Text } from 'react-native';

import { cn } from '@/lib/utils';

type ButtonProps = PressableProps & {
  label: string;
  variant?: 'primary' | 'secondary' | 'ghost';
  icon?: React.ReactNode;
  textClassName?: string;
};

const buttonVariants = {
  primary: 'bg-dashboard-primary',
  secondary: 'border border-dashboard-border bg-dashboard-surface',
  ghost: 'bg-transparent',
};

const textVariants = {
  primary: 'text-white',
  secondary: 'text-dashboard-text',
  ghost: 'text-dashboard-primary',
};

export function Button({
  className,
  disabled,
  icon,
  label,
  style,
  textClassName,
  variant = 'primary',
  ...props
}: ButtonProps) {
  return (
    <Pressable
      accessibilityRole="button"
      className={cn(
        'min-h-11 flex-row items-center justify-center gap-2 rounded-card px-4',
        buttonVariants[variant],
        disabled && 'opacity-40',
        className,
      )}
      disabled={disabled}
      style={(state) => [
        { opacity: state.pressed && !disabled ? 0.72 : disabled ? 0.4 : 1 },
        typeof style === 'function' ? style(state) : style,
      ]}
      {...props}>
      {icon}
      {label ? (
        <Text className={cn('text-sm font-semibold', textVariants[variant], textClassName)}>
          {label}
        </Text>
      ) : null}
    </Pressable>
  );
}
