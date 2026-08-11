import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { Appointment } from '../../models/Appointment ';
import { Doctor } from '../../models/doctor';
import { Slot } from '../../models/slot';
import { AppointmentService } from '../../services/appointement/appointment-service';
import { DoctorService } from '../../services/doctor/doctor-service';

interface DayOption {
  dateIso: string;
  slots: Slot[];
}

type LoadState = 'loading' | 'loaded' | 'empty' | 'error';

@Component({
  selector: 'app-book-appointment-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './book-appointment-page.html',
  styleUrl: './book-appointment-page.scss'
})
export class BookAppointmentPage implements OnInit {
  private readonly periodLength = 14;
  doctorId = 0;
  doctor: Doctor | null = null;
  doctorState: LoadState = 'loading';
  availabilityState: LoadState = 'loading';
  errorMessage = '';
  bookingError = '';
  booking = false;
  bookedAppointment: Appointment | null = null;

  periodStart = this.startOfDay(new Date());
  dayOptions: DayOption[] = [];
  selectedDay: DayOption | null = null;
  selectedSlot: Slot | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly doctorService: DoctorService,
    private readonly appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('doctorId'));
    if (!Number.isInteger(id) || id <= 0) {
      this.doctorState = 'error';
      this.availabilityState = 'error';
      this.errorMessage = 'The selected doctor could not be found.';
      return;
    }
    this.doctorId = id;
    this.loadPage();
  }

  loadPage(): void {
    this.doctorState = 'loading';
    this.errorMessage = '';
    this.doctorService.getBookableDoctorById(this.doctorId).subscribe({
      next: doctor => {
        this.doctor = doctor;
        this.doctorState = 'loaded';
      },
      error: error => {
        this.doctorState = 'error';
        this.errorMessage = error?.status === 404
          ? 'This doctor is not available for booking.'
          : 'We could not retrieve this doctor’s information.';
      }
    });
    this.loadAvailability();
  }

  loadAvailability(): void {
    this.availabilityState = 'loading';
    this.errorMessage = '';
    this.bookingError = '';
    this.selectedDay = null;
    this.selectedSlot = null;
    this.appointmentService.getBookableSlots(
      this.doctorId,
      this.dateKey(this.periodStart),
      this.dateKey(this.periodEnd)
    ).subscribe({
      next: slots => this.setSlots(slots || []),
      error: () => {
        this.availabilityState = 'error';
        this.errorMessage = 'We could not retrieve this doctor’s appointment times.';
      }
    });
  }

  private setSlots(slots: Slot[]): void {
    const grouped = new Map<string, Slot[]>();
    slots
      .filter(slot => !!slot.id && !!slot.startDateTime)
      .sort((a, b) => a.startDateTime.localeCompare(b.startDateTime))
      .forEach(slot => {
        const dateIso = slot.startDateTime.slice(0, 10);
        grouped.set(dateIso, [...(grouped.get(dateIso) || []), slot]);
      });
    this.dayOptions = Array.from(grouped, ([dateIso, dateSlots]) => ({ dateIso, slots: dateSlots }));
    this.availabilityState = this.dayOptions.length ? 'loaded' : 'empty';
  }

  selectDay(day: DayOption): void {
    this.selectedDay = day;
    this.selectedSlot = null;
    this.bookingError = '';
  }

  selectSlot(slot: Slot): void {
    this.selectedSlot = slot;
    this.bookingError = '';
  }

  previousPeriod(): void {
    if (!this.canGoPrevious) return;
    const candidate = this.addDays(this.periodStart, -this.periodLength);
    this.periodStart = candidate < this.today ? this.today : candidate;
    this.loadAvailability();
  }

  nextPeriod(): void {
    this.periodStart = this.addDays(this.periodStart, this.periodLength);
    this.loadAvailability();
  }

  confirmAppointment(): void {
    if (!this.selectedSlot?.id || this.booking) return;
    this.booking = true;
    this.bookingError = '';
    this.appointmentService.book({
      slotId: this.selectedSlot.id,
      slotStartDateTime: this.selectedSlot.startDateTime
    }).pipe(finalize(() => this.booking = false)).subscribe({
      next: appointment => this.bookedAppointment = appointment,
      error: error => {
        this.bookingError = error?.status === 409
          ? 'This time slot is no longer available. Please choose another time.'
          : (error?.error?.message || 'We could not book this appointment. Please try again.');
        if (error?.status === 409) this.loadAvailability();
      }
    });
  }

  get morningSlots(): Slot[] {
    return (this.selectedDay?.slots || []).filter(slot => Number(slot.startDateTime.slice(11, 13)) < 12);
  }

  get afternoonSlots(): Slot[] {
    return (this.selectedDay?.slots || []).filter(slot => Number(slot.startDateTime.slice(11, 13)) >= 12);
  }

  get periodEnd(): Date { return this.addDays(this.periodStart, this.periodLength - 1); }
  get today(): Date { return this.startOfDay(new Date()); }
  get canGoPrevious(): boolean { return this.periodStart.getTime() > this.today.getTime(); }
  get doctorName(): string {
    if (!this.doctor) return '';
    return `Dr. ${this.doctor.firstname} ${this.doctor.lastname}`.trim();
  }
  get doctorInitials(): string {
    return `${this.doctor?.firstname?.charAt(0) || ''}${this.doctor?.lastname?.charAt(0) || ''}`.toUpperCase() || 'DR';
  }
  get selectedDuration(): number | null {
    if (!this.selectedSlot) return null;
    const start = new Date(this.selectedSlot.startDateTime).getTime();
    const end = new Date(this.selectedSlot.endDateTime).getTime();
    return Number.isNaN(start) || Number.isNaN(end) ? null : Math.round((end - start) / 60000);
  }

  dayName(dateIso: string): string { return this.localDate(dateIso).toLocaleDateString('en-US', { weekday: 'short' }); }
  dayNumber(dateIso: string): string { return this.localDate(dateIso).toLocaleDateString('en-US', { day: '2-digit', month: 'short' }); }
  isToday(dateIso: string): boolean { return dateIso === this.dateKey(this.today); }
  timeLabel(slot: Slot): string { return slot.startDateTime.slice(11, 16); }
  longDate(dateIso?: string): string {
    return dateIso ? this.localDate(dateIso).toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' }) : '';
  }
  periodLabel(): string {
    const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric', year: 'numeric' };
    return `${this.periodStart.toLocaleDateString('en-US', options)} – ${this.periodEnd.toLocaleDateString('en-US', options)}`;
  }
  bookedDate(): string { return this.longDate(this.bookedAppointment?.slotStartDateTime?.slice(0, 10)); }
  bookedTime(): string { return this.bookedAppointment?.slotStartDateTime?.slice(11, 16) || ''; }
  trackDay(_: number, day: DayOption): string { return day.dateIso; }
  trackSlot(_: number, slot: Slot): string { return `${slot.id}-${slot.startDateTime}`; }

  private localDate(dateIso: string): Date { return new Date(`${dateIso}T00:00:00`); }
  private startOfDay(date: Date): Date { const result = new Date(date); result.setHours(0, 0, 0, 0); return result; }
  private addDays(date: Date, days: number): Date { const result = new Date(date); result.setDate(result.getDate() + days); return result; }
  private dateKey(date: Date): string {
    const pad = (value: number) => `${value}`.padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }
}
