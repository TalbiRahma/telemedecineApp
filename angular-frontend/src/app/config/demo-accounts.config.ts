export type DemoAccountRole = 'ADMIN' | 'DOCTOR' | 'PATIENT';

export interface DemoAccount {
  role: DemoAccountRole;
  label: string;
  description: string;
  email: string;
  password: string;
  icon: string;
}

interface DemoRuntimeConfig {
  enabled?: boolean;
  adminEmail?: string;
  adminPassword?: string;
  doctorEmail?: string;
  doctorPassword?: string;
  patientEmail?: string;
  patientPassword?: string;
}

declare global {
  interface Window {
    __MEDILINK_DEMO_CONFIG__?: DemoRuntimeConfig;
  }
}

export interface DemoAccountsConfiguration {
  enabled: boolean;
  accounts: readonly DemoAccount[];
}

export function getDemoAccountsConfiguration(): DemoAccountsConfiguration {
  const config = typeof window === 'undefined' ? undefined : window.__MEDILINK_DEMO_CONFIG__;
  if (!config?.enabled) return { enabled: false, accounts: [] };

  const accounts: DemoAccount[] = [
    {
      role: 'ADMIN',
      label: 'Super Admin',
      description: 'Platform administration',
      email: config.adminEmail?.trim() ?? '',
      password: config.adminPassword ?? '',
      icon: 'admin_panel_settings'
    },
    {
      role: 'DOCTOR',
      label: 'Doctor',
      description: 'Confirmed medical practitioner',
      email: config.doctorEmail?.trim() ?? '',
      password: config.doctorPassword ?? '',
      icon: 'medical_services'
    },
    {
      role: 'PATIENT',
      label: 'Patient',
      description: 'Patient portal access',
      email: config.patientEmail?.trim() ?? '',
      password: config.patientPassword ?? '',
      icon: 'person_outline'
    }
  ];

  const complete = accounts.every((account) => account.email && account.password);
  return complete ? { enabled: true, accounts } : { enabled: false, accounts: [] };
}
