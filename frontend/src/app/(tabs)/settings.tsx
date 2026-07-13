import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Card } from '@/components/ui/card';
import { AppText } from '@/components/ui/text';
import { API_BASE_URL } from '@/lib/api';
import { rooms } from '@/lib/theme';

export default function SettingsScreen() {
  return (
    <SafeAreaView className="flex-1 bg-dashboard-background" edges={['top', 'left', 'right']}>
      <ScrollView contentContainerClassName="gap-4 px-4 pb-28 pt-4">
        <View>
          <AppText variant="title">Settings</AppText>
          <AppText variant="caption">Connection and room configuration</AppText>
        </View>

        <Card className="gap-3">
          <AppText variant="label">API base URL</AppText>
          <AppText className="font-mono text-sm" selectable>
            {API_BASE_URL}
          </AppText>
        </Card>

        <Card className="gap-3">
          <AppText variant="label">Rooms</AppText>
          {rooms.map((room) => (
            <View className="flex-row items-center justify-between" key={room.id}>
              <AppText>{room.label}</AppText>
              <AppText variant="caption">{room.id}</AppText>
            </View>
          ))}
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}
