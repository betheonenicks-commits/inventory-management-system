/** Core domain types for IAMS. Replace fixtures with API responses of the same shape. */

export type AssetStatus = "In use" | "In storage" | "Under repair" | "Missing";
export type UserStatus = "Active" | "Offboarding";

export interface Asset {
  id: string;
  name: string;
  category: string;
  location: string;
  department: string;
  status: AssetStatus;
  assignee: string | null;
  cost: string;
  vendor: string;
  purchased: string;
  warranty: string | null;
  /** Category-specific custom fields (FR-AST-05). */
  fields: Record<string, string>;
}

export interface User {
  name: string;
  email: string;
  role: string;
  department: string;
  status: UserStatus;
  pendingReturns?: number;
}

export interface ActivityEvent {
  text: string;
  time: string;
  severity?: "default" | "alert";
}

export interface NavItem {
  id: string;
  name: string;
  href: string;
  icon: string;
  /** In Release 1 scope. Items outside R1 render a lock panel. */
  r1: boolean;
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}
