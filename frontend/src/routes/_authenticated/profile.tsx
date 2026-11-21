import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { UserCog, Calendar as CalendarIcon } from "lucide-react";
import { format, parse, isValid } from "date-fns";
import { toast } from "sonner";
import { PageHeader } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api } from "@/lib/api";
import { validateValues } from "@/lib/validation";
import { roleLabel } from "@/lib/auth";
import { useFarm } from "@/lib/farm-context";
import { cn } from "@/lib/utils";
import type { UserProfile } from "@/lib/types";

export const Route = createFileRoute("/_authenticated/profile")({
  head: () => ({ meta: [{ title: "Profile — AgroPrecision" }, { name: "description", content: "Your account details and password." }] }),
  component: ProfilePage,
});

function ProfilePage() {
  const { session } = useFarm();
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ["account"], queryFn: () => api<UserProfile>("/api/account") });

  const [form, setForm] = useState({
    first_name: "",
    last_name: "",
    phone_number: "",
    email: "",
    gender: "",
    date_of_birth: "",
    address: "",
    city: "",
    state: "",
    pincode: "",
  });

  const [pw, setPw] = useState({ currentPassword: "", newPassword: "" });

  useEffect(() => {
    if (data) {
      setForm({
        first_name: data.first_name ?? "",
        last_name: data.last_name ?? "",
        phone_number: data.phone_number ?? data.phone ?? "",
        email: data.email ?? "",
        gender: data.gender ?? "",
        date_of_birth: data.date_of_birth ? data.date_of_birth.slice(0, 10) : "",
        address: data.address ?? "",
        city: data.city ?? "",
        state: data.state ?? "",
        pincode: data.pincode ?? "",
      });
    }
  }, [data]);

  const save = useMutation({
    mutationFn: () =>
      api("/api/account", {
        method: "PUT",
        body: {
          firstName: form.first_name,
          lastName: form.last_name,
          phoneNumber: form.phone_number,
          gender: form.gender,
          dateOfBirth: form.date_of_birth || null,
          address: form.address,
          city: form.city,
          state: form.state,
          pincode: form.pincode,
        },
      }),
    onSuccess: () => {
      toast.success("Profile saved");
      qc.invalidateQueries({ queryKey: ["account"] });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const change = useMutation({
    mutationFn: () => api("/api/account/password", { method: "POST", body: pw }),
    onSuccess: () => {
      toast.success("Password changed");
      setPw({ currentPassword: "", newPassword: "" });
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const fieldCls =
    "h-10 w-full rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-3 text-sm text-[#123b2a] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#16834a]";

  const parsedDob = form.date_of_birth
    ? parse(form.date_of_birth.slice(0, 10), "yyyy-MM-dd", new Date())
    : undefined;
  const selectedDob = parsedDob && isValid(parsedDob) ? parsedDob : undefined;

  return (
    <div className="space-y-6">
      <PageHeader
        icon={UserCog}
        title="Profile"
        subtitle={`${session.email} · ${session.roles.map((r) => roleLabel[r]).join(", ")}`}
      />

      <div className="grid gap-6 lg:grid-cols-3">
        {/* A. Personal Information Form */}
        <form
          className="animate-rise space-y-4 rounded-2xl border bg-card p-6 shadow-soft lg:col-span-2"
          onSubmit={(e) => {
            e.preventDefault();
            const err = validateValues({ phone: form.phone_number });
            if (err) {
              toast.error(err);
              return;
            }
            save.mutate();
          }}
        >
          <div className="border-b pb-3">
            <h2 className="text-lg font-semibold text-foreground">A. Personal Information</h2>
            <p className="text-xs text-muted-foreground mt-0.5">Manage your personal details and location information.</p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-1.5">
              <Label htmlFor="first_name">First Name</Label>
              <Input
                id="first_name"
                placeholder="First name"
                className={fieldCls}
                value={form.first_name}
                onChange={(e) => setForm({ ...form, first_name: e.target.value })}
              />
            </div>

            <div className="grid gap-1.5">
              <Label htmlFor="last_name">Last Name</Label>
              <Input
                id="last_name"
                placeholder="Last name"
                className={fieldCls}
                value={form.last_name}
                onChange={(e) => setForm({ ...form, last_name: e.target.value })}
              />
            </div>

            <div className="grid gap-1.5">
              <Label htmlFor="phone_number">Phone Number</Label>
              <Input
                id="phone_number"
                placeholder="10-digit phone number"
                className={fieldCls}
                value={form.phone_number}
                onChange={(e) => setForm({ ...form, phone_number: e.target.value })}
              />
            </div>

            <div className="grid gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={form.email}
                readOnly
                disabled
                className={cn(fieldCls, "bg-[#eaf4eb] opacity-80 cursor-not-allowed")}
              />
            </div>

            <div className="grid gap-1.5">
              <Label htmlFor="gender">Gender</Label>
              <Select
                value={form.gender || undefined}
                onValueChange={(val) => setForm({ ...form, gender: val })}
              >
                <SelectTrigger id="gender" className={fieldCls}>
                  <SelectValue placeholder="Select gender" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Male">Male</SelectItem>
                  <SelectItem value="Female">Female</SelectItem>
                  <SelectItem value="Other">Other</SelectItem>
                  <SelectItem value="Prefer not to say">Prefer not to say</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-1.5">
              <Label htmlFor="date_of_birth">Date of Birth</Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    id="date_of_birth"
                    type="button"
                    variant="outline"
                    className="h-10 w-full justify-between rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-3 text-sm font-normal text-[#123b2a] hover:bg-[#eaf6ec]"
                  >
                    <span className={cn("text-left", !form.date_of_birth && "text-[#78917d]")}>
                      {selectedDob ? format(selectedDob, "dd-MM-yyyy") : "DD-MM-YYYY"}
                    </span>
                    <CalendarIcon className="ml-auto size-4 shrink-0 text-[#16834a]" />
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto border-0 bg-transparent p-0 shadow-none" align="end">
                  <Calendar
                    mode="single"
                    selected={selectedDob}
                    onSelect={(date) => {
                      if (!date) return;
                      setForm({ ...form, date_of_birth: format(date, "yyyy-MM-dd") });
                    }}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            </div>

            <div className="grid gap-1.5 sm:col-span-2">
              <Label htmlFor="address">Address</Label>
              <Input
                id="address"
                placeholder="Street address or village"
                className={fieldCls}
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
              />
            </div>

            <div className="grid gap-1.5">
              <Label htmlFor="city">City</Label>
              <Input
                id="city"
                placeholder="City / District"
                className={fieldCls}
                value={form.city}
                onChange={(e) => setForm({ ...form, city: e.target.value })}
              />
            </div>

            <div className="grid gap-1.5">
              <Label htmlFor="state">State</Label>
              <Input
                id="state"
                placeholder="State / Province"
                className={fieldCls}
                value={form.state}
                onChange={(e) => setForm({ ...form, state: e.target.value })}
              />
            </div>

            <div className="grid gap-1.5 sm:col-span-2">
              <Label htmlFor="pincode">Pincode</Label>
              <Input
                id="pincode"
                placeholder="Postal / Pincode"
                className={fieldCls}
                value={form.pincode}
                onChange={(e) => setForm({ ...form, pincode: e.target.value })}
              />
            </div>
          </div>

          <div className="pt-2">
            <Button type="submit" loading={save.isPending}>Save Details</Button>
          </div>
        </form>

        {/* Change password */}
        <form
          className="animate-rise space-y-4 rounded-2xl border bg-card p-6 shadow-soft h-fit"
          onSubmit={(e) => {
            e.preventDefault();
            if (pw.newPassword.length < 8) return toast.error("New password must be at least 8 characters.");
            change.mutate();
          }}
        >
          <div className="border-b pb-3">
            <h2 className="text-lg font-semibold text-foreground">Change Password</h2>
            <p className="text-xs text-muted-foreground mt-0.5">Ensure your account uses a strong password.</p>
          </div>

          <div className="grid gap-1.5">
            <Label htmlFor="cp">Current password</Label>
            <Input
              id="cp"
              type="password"
              required
              className={fieldCls}
              value={pw.currentPassword}
              onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })}
            />
          </div>

          <div className="grid gap-1.5">
            <Label htmlFor="np">New password</Label>
            <Input
              id="np"
              type="password"
              required
              className={fieldCls}
              value={pw.newPassword}
              onChange={(e) => setPw({ ...pw, newPassword: e.target.value })}
            />
          </div>

          <Button type="submit" variant="secondary" loading={change.isPending}>Update password</Button>
        </form>
      </div>
    </div>
  );
}
