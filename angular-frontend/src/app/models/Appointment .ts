export type AppointmentStatus = 'BOOKED' | 'CONFIRMED' | 'CANCELED' | 'COMPLETED' | 'NO_SHOW';

export interface Appointment {
  id?: number;
  bookedAt?: string;
  status?: AppointmentStatus;

  slotId: number;
  patientId?: number;

  // ✅ slot الحقيقي
  slotStartDateTime?: string;
  slotEndDateTime?: string;

  // ✅ doctor info
  doctorId?: number;
  doctorFirstname?: string;
  doctorLastname?: string;
  doctorSpecialtyName?: string;
  doctorAdresse?: string;

    doctorPhone?: string;
  patientName?: string;
}
