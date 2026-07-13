import { useRouter } from 'expo-router';
import { RefreshCw } from 'lucide-react-native';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RoomCard } from '@/components/dashboard/room-card';
import { Button } from '@/components/ui/button';
import { AppText } from '@/components/ui/text';
import { fetchLatestMeasurement, Measurement } from '@/lib/api';
import { palette, RoomId, rooms } from '@/lib/theme';

type Snapshot = {
  measurement: Measurement | null;
  status: 'loading' | 'ready' | 'empty' | 'error';
};

type SnapshotState = Record<RoomId, Snapshot>;

function createInitialState(): SnapshotState {
  return rooms.reduce((state, room) => {
    state[room.id] = { measurement: null, status: 'loading' };
    return state;
  }, {} as SnapshotState);
}

export default function OverviewScreen() {
  const router = useRouter();
  const [snapshots, setSnapshots] = useState<SnapshotState>(() => createInitialState());
  const [refreshing, setRefreshing] = useState(false);
  const [lastLoadedAt, setLastLoadedAt] = useState<Date | null>(null);

  const loading = useMemo(
    () => rooms.some((room) => snapshots[room.id].status === 'loading'),
    [snapshots],
  );

  const loadRooms = useCallback(async (signal?: AbortSignal) => {
    setSnapshots((current) =>
      rooms.reduce((state, room) => {
        state[room.id] = {
          measurement: current[room.id]?.measurement ?? null,
          status: 'loading',
        };
        return state;
      }, {} as SnapshotState),
    );

    const results = await Promise.all(
      rooms.map(async (room) => {
        try {
          const measurement = await fetchLatestMeasurement(room.id, signal);
          return {
            room: room.id,
            snapshot: {
              measurement,
              status: measurement ? 'ready' : 'empty',
            } satisfies Snapshot,
          };
        } catch {
          if (signal?.aborted) {
            return null;
          }

          return {
            room: room.id,
            snapshot: {
              measurement: null,
              status: 'error',
            } satisfies Snapshot,
          };
        }
      }),
    );

    if (signal?.aborted) {
      return;
    }

    setSnapshots((current) => {
      const next = { ...current };
      results.forEach((result) => {
        if (result) {
          next[result.room] = result.snapshot;
        }
      });
      return next;
    });
    setLastLoadedAt(new Date());
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const timeout = setTimeout(() => {
      loadRooms(controller.signal);
    }, 0);

    return () => {
      clearTimeout(timeout);
      controller.abort();
    };
  }, [loadRooms]);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await loadRooms();
    } finally {
      setRefreshing(false);
    }
  }, [loadRooms]);

  return (
    <SafeAreaView className="flex-1 bg-dashboard-background" edges={['top', 'left', 'right']}>
      <ScrollView
        contentContainerClassName="gap-4 px-4 pb-28 pt-4"
        refreshControl={
          <RefreshControl refreshing={refreshing} tintColor={palette.primary} onRefresh={refresh} />
        }>
        <View className="flex-row items-start justify-between gap-3">
          <View className="min-w-0 flex-1">
            <AppText variant="title">Rooms</AppText>
            <AppText variant="caption">
              {lastLoadedAt ? `Updated ${lastLoadedAt.toLocaleTimeString()}` : 'Latest readings'}
            </AppText>
          </View>
          <Button
            accessibilityLabel="Refresh rooms"
            className="h-11 w-11 px-0"
            icon={<RefreshCw color="#FFFFFF" size={18} />}
            label=""
            onPress={refresh}
          />
        </View>

        {rooms.map((room) => (
          <RoomCard
            key={room.id}
            label={room.label}
            measurement={snapshots[room.id].measurement}
            onPress={() =>
              router.push({
                pathname: '/rooms/[room]',
                params: { room: room.id },
              })
            }
            status={snapshots[room.id].status}
          />
        ))}

        {!loading && rooms.every((room) => snapshots[room.id].status === 'empty') ? (
          <View className="rounded-card border border-dashboard-border bg-dashboard-surface p-4">
            <AppText className="text-center" variant="caption">
              No data yet
            </AppText>
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
