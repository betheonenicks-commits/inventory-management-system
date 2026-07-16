import type { Config } from "tailwindcss";

/**
 * IAMS design tokens.
 * All colors resolve through CSS variables defined in app/globals.css,
 * so dark mode is a pure CSS-variable swap (class strategy via next-themes).
 */
const config: Config = {
  darkMode: ["class"],
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: {
          DEFAULT: "hsl(var(--ink))",
          2: "hsl(var(--ink-2))",
          border: "hsl(var(--ink-border))",
          dim: "hsl(var(--ink-text-dim))",
        },
        paper: "hsl(var(--paper))",
        surface: "hsl(var(--surface))",
        hairline: "hsl(var(--hairline))",
        slate: { DEFAULT: "hsl(var(--slate))", dim: "hsl(var(--slate-dim))" },
        foreground: "hsl(var(--text))",
        gold: {
          DEFAULT: "hsl(var(--gold))",
          dim: "hsl(var(--gold-dim))",
          deep: "hsl(var(--gold-deep))",
        },
        verify: {
          DEFAULT: "hsl(var(--verify))",
          dim: "hsl(var(--verify-dim))",
          deep: "hsl(var(--verify-deep))",
        },
        rust: { DEFAULT: "hsl(var(--rust))", dim: "hsl(var(--rust-dim))" },
        amber: { DEFAULT: "hsl(var(--amber))", dim: "hsl(var(--amber-dim))" },
      },
      fontFamily: {
        sans: ["var(--font-sans)"],
        mono: ["var(--font-mono)"],
      },
      borderRadius: { card: "12px", field: "8px" },
    },
  },
  plugins: [],
};
export default config;
