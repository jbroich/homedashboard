export const palette = {
  surface: '#FFFFFF',
  primary: '#0F766E',
  temperature: '#E76F51',
  humidity: '#3A86FF',
  warning: '#F59E0B',
  text: '#1F2933',
  muted: '#667085',
  border: '#E2E8F0',
};

export const rooms = [
  { id: 'livingroom', label: 'Living Room' },
  { id: 'bedroom', label: 'Bedroom' },
  { id: 'office', label: 'Office' },
  { id: 'kitchen', label: 'Kitchen' },
  { id: 'toilet', label: 'Toilet' },
] as const;

export type RoomId = (typeof rooms)[number]['id'];

export const roomById = rooms.reduce(
  (lookup, room) => {
    lookup[room.id] = room;
    return lookup;
  },
  {} as Record<RoomId, (typeof rooms)[number]>,
);

export function isRoomId(value: string): value is RoomId {
  return Object.prototype.hasOwnProperty.call(roomById, value);
}
