import type { Asset, User, ActivityEvent, NavGroup } from "./types";

/**
 * Typed demo fixtures. The UI consumes only these exports, so swapping in a
 * real data layer means replacing this module (or its imports) with fetch
 * hooks that return the same shapes — no component changes required.
 */

export const assets: Asset[] = [
  { id: "AST-00482", name: "Dell Latitude 5440", category: "IT equipment", location: "Room 204", department: "IT Services", status: "In use", assignee: "Elena Marsh", cost: "1,180.00", vendor: "TechSupply Wholesale", purchased: "2025-11-02", warranty: "2028-11-02", fields: { "Serial number": "DL5440-8827", OS: "Windows 11", RAM: "16GB" } },
  { id: "AST-00483", name: "Epson PowerLite Projector", category: "AV equipment", location: "Room 101", department: "Science", status: "In use", assignee: null, cost: "640.00", vendor: "ProAV Distributors", purchased: "2024-08-15", warranty: "2026-08-15", fields: { Lumens: "3600", Resolution: "1080p" } },
  { id: "AST-00190", name: "Steel Folding Chairs (x40)", category: "Furniture", location: "Storage B", department: "Facilities", status: "In storage", assignee: null, cost: "1,200.00", vendor: "Campus Furnishings Co.", purchased: "2023-02-10", warranty: null, fields: { Material: "Steel", Color: "Black" } },
  { id: "AST-00071", name: "Ford Transit Van", category: "Vehicle", location: "Motor Pool", department: "Facilities", status: "Under repair", assignee: null, cost: "38,500.00", vendor: "Fleet Motors Inc.", purchased: "2022-05-20", warranty: "2027-05-20", fields: { VIN: "1FTBW2CM5NKA12345", Registration: "STATE-8827JX", Mileage: "42,180" } },
  { id: "AST-00512", name: "Thermo Fisher Centrifuge", category: "Lab equipment", location: "Chemistry Lab", department: "Science", status: "In use", assignee: "Dr. Adaeze Osei", cost: "4,200.00", vendor: "LabChem Supply", purchased: "2025-01-18", warranty: "2028-01-18", fields: { "Calibration due": "2026-09-01", "Hazard class": "None" } },
  { id: "AST-00615", name: "Yamaha Upright Piano", category: "Musical instrument", location: "Boardroom", department: "Administration", status: "In use", assignee: null, cost: "5,600.00", vendor: "Harmony Music Supply", purchased: "2021-09-01", warranty: null, fields: { "Instrument type": "Upright piano", "Serial number": "YM-771234" } },
  { id: "AST-00701", name: "HP LaserJet Printer", category: "IT equipment", location: "Office A10", department: "Administration", status: "Missing", assignee: "Marcus Webb", cost: "320.00", vendor: "TechSupply Wholesale", purchased: "2024-03-12", warranty: "2026-03-12", fields: { "Serial number": "HPLJ-99213" } },
  { id: "AST-00722", name: "Bosch Cordless Drill Set", category: "Tools", location: "Storage A", department: "Facilities", status: "In storage", assignee: null, cost: "210.00", vendor: "Campus Furnishings Co.", purchased: "2025-06-01", warranty: "2027-06-01", fields: { "Power source": "Battery (18V)" } },
];

export const users: User[] = [
  { name: "Priya Nandakumar", email: "priya.n@example.org", role: "Super Administrator", department: "IT Services", status: "Active" },
  { name: "Marcus Webb", email: "marcus.w@example.org", role: "Administrator", department: "Administration", status: "Active" },
  { name: "Elena Marsh", email: "elena.m@example.org", role: "Inventory Manager", department: "Facilities", status: "Active" },
  { name: "Devon Raike", email: "devon.r@example.org", role: "Auditor", department: "Finance & Accounting", status: "Active" },
  { name: "Dr. Adaeze Osei", email: "a.osei@example.org", role: "Department Head", department: "Science", status: "Active" },
  { name: "Sam Ortiz", email: "sam.ortiz@example.org", role: "Volunteer", department: "Volunteer Programs", status: "Active" },
  { name: "J. Ferreira", email: "j.ferreira@example.org", role: "Volunteer", department: "Volunteer Programs", status: "Offboarding", pendingReturns: 1 },
];

export const recentActivity: ActivityEvent[] = [
  { text: "AST-00482 assigned to Elena Marsh", time: "2 hours ago" },
  { text: "Report exported: Asset register", time: "Yesterday" },
  { text: "AST-00701 flagged Missing", time: "3 days ago", severity: "alert" },
  { text: "J. Ferreira offboarding started", time: "4 days ago" },
];

export const navigation: NavGroup[] = [
  { label: "Overview", items: [{ id: "dsh-01", name: "Dashboard", href: "/dashboard", icon: "LayoutDashboard", r1: true }] },
  { label: "Assets", items: [
    { id: "ast-01", name: "Asset list", href: "/assets", icon: "Package", r1: true },
    { id: "ast-04", name: "Label queue", href: "/labels", icon: "Tag", r1: true },
  ]},
  { label: "Inventory", items: [{ id: "inv-01", name: "Stock list", href: "/stock", icon: "Boxes", r1: false }] },
  { label: "Lifecycle", items: [{ id: "lif-02", name: "Transfers", href: "/transfers", icon: "ArrowLeftRight", r1: false }] },
  { label: "Audits", items: [{ id: "aud-01", name: "Audit list", href: "/audits", icon: "ClipboardCheck", r1: false }] },
  { label: "Reports", items: [{ id: "rpt-01", name: "Report catalog", href: "/reports", icon: "FileText", r1: true }] },
  { label: "Organization", items: [{ id: "org-01", name: "Hierarchy", href: "/hierarchy", icon: "Network", r1: true }] },
  { label: "Users", items: [{ id: "usr-01", name: "Users", href: "/users", icon: "Users", r1: true }] },
  { label: "Security", items: [
    { id: "sec-01", name: "Activity log", href: "/activity", icon: "Shield", r1: true },
    { id: "cmp-01", name: "Retention policy", href: "/retention", icon: "ShieldCheck", r1: true },
  ]},
  { label: "Integrations", items: [{ id: "int-01", name: "Integrations", href: "/integrations", icon: "Plug", r1: false }] },
];
