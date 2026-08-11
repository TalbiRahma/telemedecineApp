import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { DoctorAvailabilityService } from '../../services/availability/doctor-availability';
import { SlotService } from '../../services/slot/slot-service';

import { Availability, AvailabilityType } from '../../models/availability';
import { Slot } from '../../models/slot';

type UiOccurrence = {
  id: number;              // availability id (rule id)
  date: string;            // occurrence date YYYY-MM-DD
  startTime: string;
  endTime: string;
  type: AvailabilityType;
  slotDuration?: number;
};

@Component({
  selector: 'app-doctor-availability',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './doctor-availability.html',
  styleUrls: ['./doctor-availability.scss']
})
export class DoctorAvailability implements OnInit {

  weekDays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  selectedDay = 'Monday';

  startTime = '';
  endTime = '';
  slotDuration = 30;
  type: AvailabilityType = 'RECURRING';

  rules: Availability[] = [];
  occurrences: Availability[] = [];
  dayOccurrences: UiOccurrence[] = [];

  weekFrom = '';
  weekTo = '';

  loading = false;
  errorMsg = '';
  successMsg = '';

  selectedAvailabilityId: number | null = null;
  availabilitySlots: Slot[] = [];
  loadingSlots = false;

  // -----------------------
  // ✅ Slots pagination
  // -----------------------
  slotsPage = 1;
  slotsPageSize = 5;

  // -----------------------
  // ✅ Weekly modal
  // -----------------------
  weekModalOpen = false;
  weekModalDay = 'Monday';
  weekModalOccurrences: UiOccurrence[] = [];

  // =======================
// ✅ Modal slots (inside weekly modal)
// =======================
  modalSlotsAvailabilityId: number | null = null;
  modalSlots: Slot[] = [];
  modalLoadingSlots = false;
  modalSlotsError = '';

  // pagination (modal)
  modalSlotsPage = 1;
  modalSlotsPageSize = 5;

  constructor(
    private availabilityService: DoctorAvailabilityService,
    private slotService: SlotService
  ) {}

  ngOnInit(): void {
    const { from, to } = this.getCurrentWeekRange();
    this.weekFrom = from;
    this.weekTo = to;

    this.loadRulesAndOccurrences();
  }

  // -----------------------------
  // LOAD
  // -----------------------------
  loadRulesAndOccurrences() {
    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.availabilityService.getAllRules().subscribe({
      next: (rules) => {
        this.rules = rules || [];
        this.loadOccurrencesForWeek();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Failed to load availabilities.';
        this.loading = false;
      }
    });
  }

  loadOccurrencesForWeek() {
    this.availabilityService.getOccurrences(this.weekFrom, this.weekTo, false).subscribe({
      next: (data) => {
        this.occurrences = data || [];
        this.refreshSelectedDayList();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Failed to load occurrences.';
        this.loading = false;
      }
    });
  }

  // -----------------------------
  // DAY SELECT
  // -----------------------------
  selectDay(day: string) {
    this.selectedDay = day;
    this.selectedAvailabilityId = null;
    this.availabilitySlots = [];
    this.refreshSelectedDayList();
  }

  // used by modal button
  selectDayAndJump(day: string) {
    this.selectDay(day);
    // (optional) scroll to top
    window?.scrollTo?.({ top: 0, behavior: 'smooth' });
  }

  private refreshSelectedDayList() {
    const targetIndex = this.weekDays.indexOf(this.selectedDay);

    const filtered = (this.occurrences || [])
      .filter(o => this.getWeekDayIndex(o.date!) === targetIndex)
      .sort((a, b) => (`${a.date}T${a.startTime}`).localeCompare(`${b.date}T${b.startTime}`));

    this.dayOccurrences = filtered.map(o => ({
      id: o.id!,
      date: o.date!,
      startTime: this.toHHmm(o.startTime),
      endTime: this.toHHmm(o.endTime),
      type: o.type,
      slotDuration: o.slotDuration
    }));
  }

  // -----------------------------
  // ADD AVAILABILITY
  // -----------------------------
  addSlot() {
    this.errorMsg = '';
    this.successMsg = '';

    if (!this.startTime || !this.endTime) {
      this.errorMsg = 'Please fill start and end time.';
      return;
    }
    if (!this.slotDuration || this.slotDuration <= 0) {
      this.errorMsg = 'Slot duration must be > 0.';
      return;
    }

    const dto: Availability = {
      startTime: this.toHHmm(this.startTime),
      endTime: this.toHHmm(this.endTime),
      slotDuration: this.slotDuration,
      type: this.type
    };

    if (this.type === 'ONE_OFF') {
      const dateISO = this.getNextDateForWeekday(this.selectedDay);
      dto.date = dateISO;
    } else {
      dto.dayOfWeek = this.toBackendDayOfWeek(this.selectedDay);
      dto.startDate = this.weekFrom;
      dto.endDate = undefined;
    }

    this.loading = true;

    this.availabilityService.add(dto).subscribe({
      next: () => {
        this.successMsg = 'Availability saved.';
        this.startTime = '';
        this.endTime = '';
        this.selectedAvailabilityId = null;
        this.availabilitySlots = [];
        this.loadRulesAndOccurrences();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = err?.error?.message || 'Failed to save availability.';
        this.loading = false;
      }
    });
  }

  // -----------------------------
  // REMOVE RULE
  // -----------------------------
  removeAvailability(availabilityId: number) {
    this.errorMsg = '';
    this.successMsg = '';
    this.loading = true;

    this.availabilityService.delete(availabilityId).subscribe({
      next: () => {
        this.successMsg = 'Availability deleted.';
        this.selectedAvailabilityId = null;
        this.availabilitySlots = [];
        this.loadRulesAndOccurrences();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Failed to delete availability.';
        this.loading = false;
      }
    });
  }

  // -----------------------------
  // VIEW SLOTS + ✅ PAGINATION RESET
  // -----------------------------
  openAvailabilitySlots(availabilityId: number) {
    this.selectedAvailabilityId = availabilityId;
    this.loadingSlots = true;
    this.availabilitySlots = [];
    this.resetSlotsPagination();

    this.slotService.getByAvailability(availabilityId).subscribe({
      next: (slots) => {
        this.availabilitySlots = (slots || []).slice().sort((a, b) =>
          (a.startDateTime || '').localeCompare(b.startDateTime || '')
        );
        this.loadingSlots = false;
        this.clampSlotsPage();
      },
      error: (err) => {
        console.error(err);
        this.loadingSlots = false;
      }
    });
  }

  // -----------------------
  // ✅ Slots pagination getters
  // -----------------------
  get slotsTotalPages(): number {
    const total = this.availabilitySlots?.length || 0;
    return Math.max(1, Math.ceil(total / this.slotsPageSize));
  }

  get slotsStartIndex(): number {
    return (this.slotsPage - 1) * this.slotsPageSize;
  }

  get slotsEndIndex(): number {
    const total = this.availabilitySlots?.length || 0;
    return Math.min(total, this.slotsStartIndex + this.slotsPageSize);
  }

  get pagedAvailabilitySlots(): Slot[] {
    return (this.availabilitySlots || []).slice(this.slotsStartIndex, this.slotsEndIndex);
  }

  resetSlotsPagination() {
    this.slotsPage = 1;
  }

  prevSlotsPage() {
    this.slotsPage = Math.max(1, this.slotsPage - 1);
  }

  nextSlotsPage() {
    this.slotsPage = Math.min(this.slotsTotalPages, this.slotsPage + 1);
  }

  private clampSlotsPage() {
    this.slotsPage = Math.min(this.slotsPage, this.slotsTotalPages);
    this.slotsPage = Math.max(1, this.slotsPage);
  }

  // ✅ slot status badge classes
  slotStatusClass(status?: string): string {
    const s = (status || '').toUpperCase();
    return s === 'FREE'
      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
      : s === 'UNAVAILABLE'
        ? 'bg-rose-50 text-rose-700 border-rose-200'
        : 'bg-slate-50 text-slate-700 border-slate-200';
  }

  // -----------------------------
  // WEEK SUMMARY helper
  // -----------------------------
  getOccurrencesForDay(day: string): UiOccurrence[] {
    const target = this.weekDays.indexOf(day);

    return (this.occurrences || [])
      .filter(o => this.getWeekDayIndex(o.date!) === target)
      .sort((a, b) => (`${a.date}T${a.startTime}`).localeCompare(`${b.date}T${b.startTime}`))
      .map(o => ({
        id: o.id!,
        date: o.date!,
        startTime: this.toHHmm(o.startTime),
        endTime: this.toHHmm(o.endTime),
        type: o.type,
        slotDuration: o.slotDuration
      }));
  }

  // -----------------------
  // ✅ Weekly modal handlers
  // -----------------------
 openWeekDayModal(day: string) {
  this.weekModalDay = day;
  this.weekModalOccurrences = this.getOccurrencesForDay(day);
  this.weekModalOpen = true;

  // reset modal slots
  this.modalSlotsAvailabilityId = null;
  this.modalSlots = [];
  this.modalSlotsError = '';
  this.modalLoadingSlots = false;
  this.resetModalSlotsPagination();
}

closeWeekDayModal() {
  this.weekModalOpen = false;
  this.weekModalOccurrences = [];

  // reset modal slots
  this.modalSlotsAvailabilityId = null;
  this.modalSlots = [];
  this.modalSlotsError = '';
  this.modalLoadingSlots = false;
  this.resetModalSlotsPagination();
}


  // -----------------------------
  // Helpers
  // -----------------------------
  private toHHmm(t: string): string {
    if (!t) return t;
    return t.length >= 5 ? t.slice(0, 5) : t;
  }

  private getWeekDayIndex(dateISO: string): number {
    const d = new Date(dateISO + 'T00:00:00');
    const js = d.getDay(); // 0 Sunday..6 Saturday
    return js === 0 ? 6 : js - 1; // Monday=0 .. Sunday=6
  }

  private getNextDateForWeekday(dayName: string): string {
    const targetIndex = this.weekDays.indexOf(dayName);
    const todayISO = new Date().toISOString().slice(0, 10);
    const todayIndex = this.getWeekDayIndex(todayISO);

    let diff = targetIndex - todayIndex;
    if (diff < 0) diff += 7;

    const next = new Date();
    next.setDate(next.getDate() + diff);

    return next.toISOString().slice(0, 10);
  }

  private toBackendDayOfWeek(uiDay: string): string {
    return uiDay.toUpperCase();
  }

  private getCurrentWeekRange(): { from: string; to: string } {
    const now = new Date();
    const todayISO = now.toISOString().slice(0, 10);
    const idx = this.getWeekDayIndex(todayISO);

    const monday = new Date(now);
    monday.setDate(now.getDate() - idx);

    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);

    return {
      from: monday.toISOString().slice(0, 10),
      to: sunday.toISOString().slice(0, 10)
    };
  }

  // =======================
// ✅ Modal slots (inside weekly modal)
// =======================
get modalSlotsTotalPages(): number {
  const total = this.modalSlots?.length || 0;
  return Math.max(1, Math.ceil(total / this.modalSlotsPageSize));
}

get modalSlotsStartIndex(): number {
  return (this.modalSlotsPage - 1) * this.modalSlotsPageSize;
}

get modalSlotsEndIndex(): number {
  const total = this.modalSlots?.length || 0;
  return Math.min(total, this.modalSlotsStartIndex + this.modalSlotsPageSize);
}

get pagedModalSlots(): Slot[] {
  return (this.modalSlots || []).slice(this.modalSlotsStartIndex, this.modalSlotsEndIndex);
}

resetModalSlotsPagination() {
  this.modalSlotsPage = 1;
}

prevModalSlotsPage() {
  this.modalSlotsPage = Math.max(1, this.modalSlotsPage - 1);
}

nextModalSlotsPage() {
  this.modalSlotsPage = Math.min(this.modalSlotsTotalPages, this.modalSlotsPage + 1);
}

private clampModalSlotsPage() {
  this.modalSlotsPage = Math.min(this.modalSlotsPage, this.modalSlotsTotalPages);
  this.modalSlotsPage = Math.max(1, this.modalSlotsPage);
}

toggleModalSlots(availabilityId: number) {
  // if already open => close
  if (this.modalSlotsAvailabilityId === availabilityId) {
    this.modalSlotsAvailabilityId = null;
    this.modalSlots = [];
    this.modalSlotsError = '';
    this.modalLoadingSlots = false;
    return;
  }

  // open + load
  this.modalSlotsAvailabilityId = availabilityId;
  this.modalSlots = [];
  this.modalSlotsError = '';
  this.modalLoadingSlots = true;
  this.resetModalSlotsPagination();

  this.slotService.getByAvailability(availabilityId).subscribe({
    next: (slots) => {
      this.modalSlots = (slots || []).slice().sort((a, b) =>
        (a.startDateTime || '').localeCompare(b.startDateTime || '')
      );
      this.modalLoadingSlots = false;
      this.clampModalSlotsPage();
    },
    error: (err) => {
      console.error(err);
      this.modalSlotsError = 'Failed to load slots.';
      this.modalLoadingSlots = false;
    }
  });
}


}
