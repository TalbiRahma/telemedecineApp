export type SlotStatus = 'FREE' | 'UNAVAILABLE';

export interface Slot {
  id?: number;

  // ✅ same as backend SlotDto
  startDateTime: string; // "2026-01-20T09:00:00"
  endDateTime: string;   // "2026-01-20T09:30:00"

  status: SlotStatus;

  availabilityId?: number;
}
