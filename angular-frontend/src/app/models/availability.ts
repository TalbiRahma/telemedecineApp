import { Slot } from "./slot";

export type AvailabilityType = 'RECURRING' | 'ONE_OFF';

export interface Availability {
  id?: number;
  doctorId?: number;

  // ONE_OFF
  date?: string; // "YYYY-MM-DD"

  // RECURRING
  dayOfWeek?: string; // "MONDAY" .. "SUNDAY"
  startDate?: string; // "YYYY-MM-DD"
  endDate?: string;   // "YYYY-MM-DD" optional

  // common
  startTime: string; // "HH:mm"
  endTime: string;   // "HH:mm"
  type: AvailabilityType;
  slotDuration?: number;

  // optional response (if includeSlots=true)
  slots?: Slot[];
}
