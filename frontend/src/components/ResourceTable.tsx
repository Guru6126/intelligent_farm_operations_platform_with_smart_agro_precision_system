
import { useMemo, useState, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Pencil,
  Plus,
  Trash2,
  Download,
  AlertCircle,
  Calendar as CalendarIcon,
  Clock,
} from "lucide-react";
import { toast } from "sonner";
import { format, parse, isValid } from "date-fns";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

import {
  deleteRow,
  downloadCsv,
  insertRow,
  listRows,
  updateRow,
} from "@/services/crud";
import { validateValues } from "@/lib/validation";
import { useFarm } from "@/lib/farm-context";
import { canManage } from "@/lib/auth";
import { cn } from "@/lib/utils";

const EMPTY_SELECT_VALUE = "__empty__";

export type FieldType =
  | "text"
  | "number"
  | "date"
  | "datetime"
  | "select"
  | "ref"
  | "textarea";

export interface FieldDef {
  key: string;
  label: string;
  type?: FieldType;
  required?: boolean;
  options?: string[];
  ref?: { table: string; label: string };
  default?: unknown;
}

export interface ColumnDef<T> {
  key: string;
  label: string;
  render?: (row: T, refs: Record<string, Record<string, string>>) => ReactNode;
}

export interface ResourceConfig<T> {
  table: string;
  title: string;
  singular: string;
  fields: FieldDef[];
  columns: ColumnDef<T>[];
  orderBy?: string;
  farmScoped?: boolean;
  writeAccess?: "manage" | "all";
  allowEdit?: boolean;
  allowDelete?: boolean;
  validate?: (
    values: Record<string, unknown>,
    rows: T[],
  ) => string | null | undefined;
}

type Row = Record<string, unknown> & { id: string };

export function useRows<T>(
  table: string,
  orderBy = "created_at",
  farmScoped = true,
) {
  const { farmId } = useFarm();

  return useQuery({
    queryKey: [table, farmScoped ? farmId : "all"],
    queryFn: () =>
      listRows<T>(table, {
        farmId: farmScoped ? farmId : null,
        orderBy,
      }),
  });
}

function useRefLabels(fields: FieldDef[]) {
  const { farmId } = useFarm();
  const refFields = fields.filter((f) => f.type === "ref" && f.ref);

  const { data } = useQuery({
    queryKey: ["refs", farmId, refFields.map((f) => f.ref!.table).join(",")],
    enabled: refFields.length > 0,
    queryFn: async () => {
      const out: Record<string, Record<string, string>> = {};

      for (const f of refFields) {
        const rows = await listRows<Row>(f.ref!.table, { farmId });

        out[f.key] = Object.fromEntries(
          rows.map((r) => [r.id, String(r[f.ref!.label] ?? "")]),
        );
      }

      return out;
    },
  });

  return data ?? {};
}

export function ResourceTable<T extends Row>({
  config,
  children,
}: {
  config: ResourceConfig<T>;
  children?: ReactNode;
}) {
  const qc = useQueryClient();
  const { farmId, farms, session } = useFarm();

  const farmScoped = config.farmScoped ?? true;

  const {
    data: rows = [],
    isLoading,
    error,
  } = useRows<T>(config.table, config.orderBy, farmScoped);

  const refs = useRefLabels(config.fields);

  const [editing, setEditing] = useState<Partial<Row> | null>(null);

  const manager = canManage(session.roles);
  const canWrite = manager || config.writeAccess === "all";

  const save = useMutation({
    mutationFn: async (values: Record<string, unknown>) => {
      const { id, ...rest } = values;

      if (id) return updateRow(config.table, String(id), rest);

      return insertRow(config.table, rest);
    },
    onSuccess: () => {
      toast.success(`${config.singular} saved`);
      qc.invalidateQueries();
      setEditing(null);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const remove = useMutation({
    mutationFn: (id: string) => deleteRow(config.table, id),
    onSuccess: () => {
      toast.success(`${config.singular} deleted`);
      qc.invalidateQueries();
    },
    onError: (e: Error) => toast.error(e.message),
  });

  const openNew = () => {
    if (farmScoped && !farmId) {
      if (farms.length === 1) {
        return setEditing({ farm_id: farms[0].id });
      }

      return setEditing({ farm_id: farms[0]?.id });
    }

    setEditing(farmScoped ? { farm_id: farmId! } : {});
  };

  return (
    <section
      data-reveal
      className="is-visible animate-rise rounded-2xl border bg-card shadow-soft"
    >
      <header className="flex flex-wrap items-center justify-between gap-3 border-b px-5 py-4">
        <h2 className="text-lg font-semibold">{config.title}</h2>

        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => downloadCsv(`${config.table}.csv`, rows)}
            disabled={!rows.length}
          >
            <Download /> CSV
          </Button>

          {canWrite && (
            <Button
              size="sm"
              onClick={openNew}
              disabled={farmScoped && farms.length === 0}
            >
              <Plus /> Add {config.singular.toLowerCase()}
            </Button>
          )}
        </div>
      </header>

      {children}

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-secondary/60 text-left text-xs uppercase tracking-wide text-muted-foreground">
            <tr>
              {config.columns.map((c) => (
                <th key={c.key} className="px-5 py-3 font-medium">
                  {c.label}
                </th>
              ))}

              {canWrite && <th className="px-5 py-3" />}
            </tr>
          </thead>

          <tbody>
            {isLoading && (
              <tr>
                <td className="px-5 py-8 text-muted-foreground" colSpan={99}>
                  Loading…
                </td>
              </tr>
            )}

            {error && (
              <tr>
                <td className="px-5 py-8 text-destructive" colSpan={99}>
                  {(error as Error).message}
                </td>
              </tr>
            )}

            {!isLoading && !error && rows.length === 0 && (
              <tr>
                <td
                  className="px-5 py-10 text-center text-muted-foreground"
                  colSpan={99}
                >
                  {farmScoped && farms.length === 0
                    ? "Create a farm first to add records here."
                    : `No ${config.title.toLowerCase()} yet.`}
                </td>
              </tr>
            )}

            {rows.map((r, i) => (
              <tr
                key={r.id}
                className="animate-slide-in border-t transition-colors hover:bg-secondary/40"
                style={{
                  animationDelay: `${Math.min(i, 12) * 30}ms`,
                }}
              >
                {config.columns.map((c) => (
                  <td key={c.key} className="px-5 py-3 align-middle">
                    {c.render
                      ? c.render(r, refs)
                      : refs[c.key]?.[String(r[c.key])] ?? fmt(r[c.key])}
                  </td>
                ))}

                {canWrite && (
                  <td className="whitespace-nowrap px-5 py-3 text-right">
                    {(config.allowEdit ?? true) && manager && (
                      <button
                        aria-label="Edit"
                        className="rounded-md p-2 text-muted-foreground transition hover:bg-secondary hover:text-primary"
                        onClick={() => setEditing(r)}
                      >
                        <Pencil className="size-4" />
                      </button>
                    )}

                    {(config.allowDelete ?? true) && manager && (
                      <button
                        aria-label="Delete"
                        className="rounded-md p-2 text-muted-foreground transition hover:bg-destructive-soft hover:text-destructive"
                        onClick={() =>
                          confirm(
                            `Delete this ${config.singular.toLowerCase()}?`,
                          ) && remove.mutate(r.id)
                        }
                      >
                        <Trash2 className="size-4" />
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editing && (
        <RecordDialog
          config={config}
          initial={editing}
          existingRows={rows}
          onClose={() => setEditing(null)}
          saving={save.isPending}
          onSave={(v) => {
            const err = validateValues(v);

            if (err) {
              toast.error(err);
              return;
            }

            save.mutate(v);
          }}
        />
      )}
    </section>
  );
}

function fmt(v: unknown) {
  if (v === null || v === undefined || v === "") {
    return <span className="text-muted-foreground">—</span>;
  }

  if (typeof v === "boolean") return v ? "Yes" : "No";

  const s = String(v);

  if (/^\d{4}-\d{2}-\d{2}T/.test(s)) {
    return new Date(s).toLocaleString();
  }

  return s.replace(/_/g, " ");
}

function RecordDialog<T extends Row>({
  config,
  initial,
  existingRows,
  onClose,
  onSave,
  saving,
}: {
  config: ResourceConfig<T>;
  initial: Partial<Row>;
  existingRows: T[];
  onClose: () => void;
  onSave: (v: Record<string, unknown>) => void;
  saving: boolean;
}) {
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { farms } = useFarm();
  const farmScoped = config.farmScoped ?? true;

  const [values, setValues] = useState<Record<string, unknown>>(() => {
    const v: Record<string, unknown> = { ...initial };

    for (const f of config.fields) {
      if (v[f.key] === undefined && f.default !== undefined) {
        v[f.key] = f.default;
      }
    }

    return v;
  });

  const formFarm = (values.farm_id as string) ?? null;

  const refFields = config.fields.filter((f) => f.type === "ref");

  const { data: refOpts = {} } = useQuery({
    queryKey: ["ref-opts", formFarm, refFields.map((f) => f.key).join()],
    enabled: refFields.length > 0,
    queryFn: async () => {
      const out: Record<string, Row[]> = {};

      for (const f of refFields) {
        out[f.key] = await listRows<Row>(f.ref!.table, {
          farmId: formFarm,
        });
      }

      return out;
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    const out: Record<string, unknown> = {};

    if (values.id) out.id = values.id;
    if (farmScoped) out.farm_id = values.farm_id;

    for (const f of config.fields) {
      let v = values[f.key];

      if (typeof v === "string") v = v.trim();
      if (v === "" || v === undefined) v = null;

      if (f.type === "number" && v !== null) {
        v = Number(v);
      }

      if (f.type === "datetime" && v) {
        v = new Date(String(v)).toISOString();
      }

      if (v === null && !values.id) continue;

      out[f.key] = v;
    }

    if (config.validate) {
      const err = config.validate(out, existingRows);

      if (err) {
        setErrorMsg(err);
        toast.error(err);
        return;
      }
    }

    onSave(out);
  };

  const set = (k: string, v: unknown) => {
    setErrorMsg(null);
    setValues((s) => ({ ...s, [k]: v }));
  };

  const fieldCls =
    "h-10 w-full rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-3 text-sm text-[#123b2a] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#16834a]";

  const title = useMemo(
    () => `${values.id ? "Edit" : "New"} ${config.singular.toLowerCase()}`,
    [values.id, config.singular],
  );

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="w-[95vw] max-w-3xl overflow-visible">
        <DialogHeader>
          <DialogTitle className="font-display">{title}</DialogTitle>
        </DialogHeader>

        <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {errorMsg && (
            <div className="flex items-center gap-2 rounded-lg border border-destructive/30 bg-destructive-soft p-3 text-xs text-destructive sm:col-span-2">
              <AlertCircle className="size-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {farmScoped && !values.id && (
            <div className="grid gap-1.5">
              <Label>Farm</Label>

              <Select
                required
                value={String(values.farm_id ?? "") || undefined}
                onValueChange={(value) => set("farm_id", value)}
              >
                <SelectTrigger className={fieldCls}>
                  <SelectValue placeholder="Select farm" />
                </SelectTrigger>

                <SelectContent>
                  {farms.map((f) => (
                    <SelectItem key={f.id} value={f.id}>
                      {f.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {config.fields.map((f) => {
            const raw = values[f.key];

            const val =
              raw === null || raw === undefined
                ? ""
                : f.type === "datetime"
                  ? toLocalInput(String(raw))
                  : String(raw);

            const parsedDate = val
              ? parse(val.slice(0, 10), "yyyy-MM-dd", new Date())
              : undefined;

            const selectedDate =
              parsedDate && isValid(parsedDate) ? parsedDate : undefined;

            return (
              <div key={f.key} className="grid gap-1.5">
                <Label htmlFor={f.key}>
                  {f.label}
                  {f.required && (
                    <span className="text-destructive"> *</span>
                  )}
                </Label>

                {f.type === "select" ? (
                  <Select
                    required={f.required}
                    value={val || undefined}
                    onValueChange={(value) =>
                      set(
                        f.key,
                        value === EMPTY_SELECT_VALUE ? "" : value,
                      )
                    }
                  >
                    <SelectTrigger id={f.key} className={fieldCls}>
                      <SelectValue placeholder="Select an option" />
                    </SelectTrigger>

                    <SelectContent>
                      {!f.required && (
                        <SelectItem value={EMPTY_SELECT_VALUE}>
                          —
                        </SelectItem>
                      )}

                      {(f.options ?? []).map((o) => (
                        <SelectItem key={o} value={o}>
                          {o.replace(/_/g, " ")}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : f.type === "ref" ? (
                  <Select
                    required={f.required}
                    value={val || undefined}
                    onValueChange={(value) =>
                      set(
                        f.key,
                        value === EMPTY_SELECT_VALUE ? "" : value,
                      )
                    }
                  >
                    <SelectTrigger id={f.key} className={fieldCls}>
                      <SelectValue placeholder="Select an option" />
                    </SelectTrigger>

                    <SelectContent>
                      {!f.required && (
                        <SelectItem value={EMPTY_SELECT_VALUE}>
                          —
                        </SelectItem>
                      )}

                      {(refOpts[f.key] ?? []).map((o) => (
                        <SelectItem key={o.id} value={o.id}>
                          {String(o[f.ref!.label] ?? "")}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : f.type === "textarea" ? (
                  <textarea
                    id={f.key}
                    className={cn(fieldCls, "h-24 py-2")}
                    value={val}
                    onChange={(e) => set(f.key, e.target.value)}
                  />
                ) : f.type === "date" || f.type === "datetime" ? (
                  <div className="flex gap-2">
                    <Popover>
                      <PopoverTrigger asChild>
                        <Button
                          id={f.key}
                          type="button"
                          variant="outline"
                          className="h-10 w-full justify-between rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-3 text-sm font-normal text-[#123b2a] hover:bg-[#eaf6ec]"
                        >
                          <span
                            className={cn(
                              "text-left",
                              !val && "text-[#78917d]",
                            )}
                          >
                            {selectedDate
                              ? format(selectedDate, "dd-MM-yyyy")
                              : "DD-MM-YYYY"}
                          </span>

                          <CalendarIcon className="ml-auto size-4 shrink-0 text-[#16834a]" />
                        </Button>
                      </PopoverTrigger>

                      <PopoverContent
                        className="w-auto border-0 bg-transparent p-0 shadow-none"
                        align="end"
                      >
                        <Calendar
                          mode="single"
                          selected={selectedDate}
                          onSelect={(date) => {
                            if (!date) return;

                            const selectedDateValue = format(
                              date,
                              "yyyy-MM-dd",
                            );

                            if (f.type === "date") {
                              set(f.key, selectedDateValue);
                            } else {
                              const currentTime = val.includes("T")
                                ? val.slice(11, 16)
                                : "09:00";

                              set(
                                f.key,
                                `${selectedDateValue}T${currentTime}`,
                              );
                            }
                          }}
                          initialFocus
                        />
                      </PopoverContent>
                    </Popover>

                    
{f.type === "datetime" && (
  <Popover>
    <PopoverTrigger asChild>
      <Button
        type="button"
        variant="outline"
        className="h-10 w-[150px] justify-between rounded-xl border border-[#b7d8bd] bg-[#f0f8f1] px-3 text-sm font-normal text-[#123b2a] hover:bg-[#eaf6ec]"
      >
        <span className={val.includes("T") ? "text-[#123b2a]" : "text-[#78917d]"}>
          {val.includes("T")
            ? (() => {
                const h = Number(val.slice(11, 13));
                return `${String((h % 12) || 12).padStart(2, "0")}:${val.slice(14, 16)} ${h >= 12 ? "PM" : "AM"}`;
              })()
            : "HH:MM "}
        </span>
        <Clock className="ml-auto size-4 shrink-0 text-[#16834a]" />
      </Button>
    </PopoverTrigger>

    <PopoverContent
      align="end"
      className="w-[300px] rounded-xl border border-[#b7d8bd] bg-[#f7fbf7] p-4"
    >
      <p className="mb-3 text-sm font-semibold text-[#123b2a]">
        Select Time
      </p>

      <div className="flex items-center gap-2">
        <div className="flex-1">
          <label className="mb-1 block text-xs text-[#52745b]">Hour</label>
          <Select
            value={
              val.includes("T")
                ? String((Number(val.slice(11, 13)) % 12) || 12).padStart(2, "0")
                : ""
            }
            onValueChange={(hour) => {
              const date = val ? val.slice(0, 10) : format(new Date(), "yyyy-MM-dd");
              const oldHour = val.includes("T") ? Number(val.slice(11, 13)) : 9;
              const isPM = oldHour >= 12;
              const newHour = Number(hour) % 12 + (isPM ? 12 : 0);
              const minute = val.includes("T") ? val.slice(14, 16) : "00";
              set(f.key, `${date}T${String(newHour).padStart(2, "0")}:${minute}`);
            }}
          >
            <SelectTrigger className="border-[#b7d8bd] bg-white text-[#123b2a]">
              <SelectValue placeholder="HH" />
            </SelectTrigger>
            <SelectContent className="max-h-52">
              {Array.from({ length: 12 }, (_, i) => {
                const hour = String(i + 1).padStart(2, "0");
                return <SelectItem key={hour} value={hour}>{hour}</SelectItem>;
              })}
            </SelectContent>
          </Select>
        </div>

        <span className="mt-5 font-semibold text-[#16834a]">:</span>

        <div className="flex-1">
          <label className="mb-1 block text-xs text-[#52745b]">Minute</label>
          <Select
            value={val.includes("T") ? val.slice(14, 16) : ""}
            onValueChange={(minute) => {
              const date = val ? val.slice(0, 10) : format(new Date(), "yyyy-MM-dd");
              const hour = val.includes("T") ? val.slice(11, 13) : "09";
              set(f.key, `${date}T${hour}:${minute}`);
            }}
          >
            <SelectTrigger className="border-[#b7d8bd] bg-white text-[#123b2a]">
              <SelectValue placeholder="MM" />
            </SelectTrigger>
            <SelectContent className="max-h-52">
              {Array.from({ length: 60 }, (_, i) => {
                const minute = String(i).padStart(2, "0");
                return <SelectItem key={minute} value={minute}>{minute}</SelectItem>;
              })}
            </SelectContent>
          </Select>
        </div>

        <div className="flex-1">
          <label className="mb-1 block text-xs text-[#52745b]">Period</label>
          <Select
            value={
              val.includes("T")
                ? Number(val.slice(11, 13)) >= 12 ? "PM" : "AM"
                : ""
            }
            onValueChange={(period) => {
              const date = val ? val.slice(0, 10) : format(new Date(), "yyyy-MM-dd");
              const currentHour = val.includes("T") ? Number(val.slice(11, 13)) : 9;
              const hour12 = (currentHour % 12) || 12;
              const newHour = period === "PM" ? hour12 % 12 + 12 : hour12 % 12;
              const minute = val.includes("T") ? val.slice(14, 16) : "00";
              set(f.key, `${date}T${String(newHour).padStart(2, "0")}:${minute}`);
            }}
          >
            <SelectTrigger className="border-[#b7d8bd] bg-white text-[#123b2a]">
              <SelectValue placeholder="AM/PM" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="AM">AM</SelectItem>
              <SelectItem value="PM">PM</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </PopoverContent>
  </Popover>
)}

                  </div>
                ) : (
                  <Input
                    id={f.key}
                    required={f.required}
                    step="any"
                    type={f.type === "number" ? "number" : "text"}
                    value={val}
                    onChange={(e) => set(f.key, e.target.value)}
                    className={fieldCls}
                  />
                )}
              </div>
            );
          })}

          <DialogFooter className="sm:col-span-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>

            <Button type="submit" loading={saving} disabled={saving}>
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function toLocalInput(iso: string) {
  const d = new Date(iso);

  if (isNaN(d.getTime())) return "";

  const off = d.getTimezoneOffset();

  return new Date(d.getTime() - off * 60000)
    .toISOString()
    .slice(0, 16);
}

export function StatusPill({
  value,
  tone,
}: {
  value: string;
  tone?: "good" | "warn" | "bad" | "water" | "muted";
}) {
  const t = tone ?? "muted";

  const cls = {
    good: "bg-secondary text-primary border-primary/20",
    warn: "bg-sun/20 text-foreground border-sun/40",
    bad: "bg-destructive-soft text-destructive border-destructive/30",
    water: "bg-water/15 text-foreground border-water/40",
    muted: "bg-muted text-muted-foreground border-border",
  }[t];

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium capitalize",
        cls,
      )}
    >
      {value.replace(/_/g, " ")}
    </span>
  );
}
