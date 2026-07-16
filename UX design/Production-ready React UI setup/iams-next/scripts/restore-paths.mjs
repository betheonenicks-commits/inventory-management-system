// Restores Next.js special folder names that can't survive zip-safe packaging:
//   app/-app-  -> app/(app)     (route group)
//   assets/-id- -> assets/[id]  (dynamic segment)
// Run once after unzipping:  node scripts/restore-paths.mjs
import { renameSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const renames = [
  [join(root, "app", "-app-", "assets", "-id-"), join(root, "app", "-app-", "assets", "[id]")],
  [join(root, "app", "-app-"), join(root, "app", "(app)")],
];
for (const [from, to] of renames) {
  if (existsSync(from)) {
    renameSync(from, to);
    console.log("renamed", from, "->", to);
  }
}
console.log("Done. Run: npm install && npm run dev");
