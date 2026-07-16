"use client";

// SCR-AST-03 · Register asset · FR-AST-01, 02, 05, 06
import { useRouter } from "next/navigation";
import { AnimatedPage } from "@/components/app/animated-page";
import { PageHeader } from "@/components/app/page-header";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";

export default function RegisterAssetPage() {
  const router = useRouter();

  return (
    <AnimatedPage>
      <PageHeader
        title="Register asset"
        sub={<>Screen <span className="font-mono text-xs">SCR-AST-03</span> · FR-AST-01, 02, 05, 06</>}
      />
      <Card>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            router.push("/assets");
          }}
        >
          <div className="grid max-w-[620px] grid-cols-1 gap-3.5 sm:grid-cols-2">
            <div>
              <Label htmlFor="name">Asset name</Label>
              <Input id="name" placeholder="e.g. Dell Latitude 5440" required />
            </div>
            <div>
              <Label htmlFor="category">Category</Label>
              <Select id="category">
                <option>IT equipment</option>
                <option>Furniture</option>
                <option>Vehicle</option>
                <option>Lab equipment</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="manufacturer">Manufacturer</Label>
              <Input id="manufacturer" placeholder="e.g. Dell" />
            </div>
            <div>
              <Label htmlFor="cost">Purchase cost</Label>
              <Input id="cost" inputMode="decimal" placeholder="0.00" />
            </div>
            <div>
              <Label htmlFor="vendor">Vendor</Label>
              <Input id="vendor" placeholder="e.g. TechSupply Wholesale" />
            </div>
            <div>
              <Label htmlFor="location">Location</Label>
              <Select id="location">
                <option>Room 204</option>
                <option>Room 101</option>
                <option>Storage B</option>
              </Select>
            </div>
          </div>

          <Label className="mt-4">Attachments</Label>
          <div className="max-w-[620px] rounded-[10px] border-[1.5px] border-dashed border-hairline p-6 text-center text-[13px] text-slate-dim">
            Drop a photo or invoice here, or click to browse
          </div>

          <Button type="submit" className="mt-[18px]">
            Save and generate QR code
          </Button>
        </form>
      </Card>
    </AnimatedPage>
  );
}
