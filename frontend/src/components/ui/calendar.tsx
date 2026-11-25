// "use client";

import * as React from "react";
import {
  ChevronDownIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
} from "lucide-react";
import {
  DayButton,
  DayPicker,
  getDefaultClassNames,
} from "react-day-picker";

import { cn } from "@/lib/utils";
import { Button, buttonVariants } from "@/components/ui/button";

type CalendarProps = React.ComponentProps<typeof DayPicker> & {
  buttonVariant?: React.ComponentProps<typeof Button>["variant"];
  /** Show month selector dropdown */
  showMonthDropdown?: boolean;
  /** Show year selector dropdown */
  showYearDropdown?: boolean;
};

function Calendar({
  className,
  classNames,
  showOutsideDays = true,
  captionLayout = "dropdown",
  buttonVariant = "ghost",
  showMonthDropdown = false,
  showYearDropdown = false,
  formatters,
  components,
  ...props
}: CalendarProps) {
  const defaultClassNames = getDefaultClassNames();

  return (
    <DayPicker
      showOutsideDays={showOutsideDays}


      className={cn(
        "bg-[#f7fbf7] text-[#123b2a] group/calendar p-3 rounded-xl border border-[#d5e8d8] shadow-md [--cell-size:2.25rem]",
        String.raw`rtl:**:[.rdp-button\_next>svg]:rotate-180`,
        String.raw`rtl:**:[.rdp-button\_previous>svg]:rotate-180`,
        className,
      )}
      captionLayout={captionLayout}
      formatters={{
        formatMonthDropdown: (date) =>
          date.toLocaleString("default", { month: "short" }),
        formatYearDropdown: (date) => date.getFullYear().toString(),
        ...formatters,
      }}
      classNames={{
        root: cn("w-fit", defaultClassNames.root),
        months: cn(
          "relative flex flex-col gap-4 md:flex-row",
          defaultClassNames.months,
        ),
        month: cn(
          "flex w-full flex-col gap-4",
          defaultClassNames.month,
        ),
        nav: cn(
          "absolute inset-x-0 top-0 flex w-full items-center justify-between gap-1",
          defaultClassNames.nav,
        ),
        button_previous: cn(
          buttonVariants({ variant: buttonVariant }),
          "h-(--cell-size) w-(--cell-size) select-none p-0 text-[#16834a] hover:bg-[#dff1e2] hover:text-[#14532d] rounded-lg",
          defaultClassNames.button_previous,
        ),
        button_next: cn(
          buttonVariants({ variant: buttonVariant }),
          "h-(--cell-size) w-(--cell-size) select-none p-0 text-[#16834a] hover:bg-[#dff1e2] hover:text-[#14532d] rounded-lg",
          defaultClassNames.button_next,
        ),
        month_caption: cn(
          "flex h-(--cell-size) w-full items-center justify-center px-(--cell-size) text-[#123b2a] font-semibold",
          defaultClassNames.month_caption,
        ),
        dropdowns: cn(
          "flex h-(--cell-size) w-full items-center justify-center gap-2 text-sm font-medium",
          defaultClassNames.dropdowns,
        ),
        dropdown_root: cn(
          "relative rounded-lg border border-[#b7d8bd] bg-[#dff1e2] text-[#123b2a] shadow-sm focus-within:border-[#16834a] focus-within:ring-2 focus-within:ring-[#16834a]/20",
          defaultClassNames.dropdown_root,
        ),
        dropdown: cn(
          "absolute inset-0 bg-[#f0f8f1] text-[#123b2a] opacity-0",
          defaultClassNames.dropdown,
        ),
        caption_label: cn(
          "select-none font-semibold text-[#123b2a]",
          captionLayout === "label"
            ? "text-sm"
            : "[&>svg]:text-[#16834a] flex h-8 items-center gap-1 rounded-lg pl-2 pr-1 text-sm",
          defaultClassNames.caption_label,
        ),
        table: "w-full border-collapse",
        weekdays: cn("flex", defaultClassNames.weekdays),
        weekday: cn(
          "flex-1 select-none rounded-md text-[0.8rem] font-medium text-[#52745d]",
          defaultClassNames.weekday,
        ),
        week: cn("mt-2 flex w-full", defaultClassNames.week),
        week_number_header: cn(
          "w-(--cell-size) select-none",
          defaultClassNames.week_number_header,
        ),
        week_number: cn(
          "select-none text-[0.8rem] text-[#78917d]",
          defaultClassNames.week_number,
        ),
        day: cn(
          "group/day relative aspect-square h-full w-full select-none p-0 text-center rounded-lg",
          defaultClassNames.day,
        ),
        range_start: cn(
          "rounded-l-lg bg-[#dff1e2]",
          defaultClassNames.range_start,
        ),
        range_middle: cn(
          "rounded-none bg-[#eaf6ec]",
          defaultClassNames.range_middle,
        ),
        range_end: cn(
          "rounded-r-lg bg-[#dff1e2]",
          defaultClassNames.range_end,
        ),
        today: cn(
          "rounded-lg bg-[#e3f2e5] text-[#16834a] font-bold",
          defaultClassNames.today,
        ),
        outside: cn(
          "text-[#a5b8a8] aria-selected:text-[#a5b8a8]",
          defaultClassNames.outside,
        ),
        disabled: cn(
          "text-[#b8c8ba] opacity-40 cursor-not-allowed",
          defaultClassNames.disabled,
        ),
        hidden: cn("invisible", defaultClassNames.hidden),
        ...classNames,
      }}
      components={{
        Root: ({ className, rootRef, ...props }) => (
          <div data-slot="calendar" ref={rootRef} className={cn(className)} {...props} />
        ),
        Chevron: ({ className, orientation, ...props }) => {
          if (orientation === "left") {
            return <ChevronLeftIcon className={cn("size-4", className)} {...props} />;
          }
          if (orientation === "right") {
            return <ChevronRightIcon className={cn("size-4", className)} {...props} />;
          }
          return <ChevronDownIcon className={cn("size-4", className)} {...props} />;
        },
        DayButton: CalendarDayButton,
        WeekNumber: ({ children, ...props }) => (
          <td {...props}>
            <div className="flex size-(--cell-size) items-center justify-center text-center">
              {children}
            </div>
          </td>
        ),
        ...components,
      }}
      {...props}
    />
  );
}

function CalendarDayButton({
  className,
  day,
  modifiers,
  ...props
}: React.ComponentProps<typeof DayButton>) {
  const defaultClassNames = getDefaultClassNames();
  const ref = React.useRef<HTMLButtonElement>(null);

  React.useEffect(() => {
    if (modifiers["focused"]) {
      ref.current?.focus();
    }
  }, [modifiers]);

  return (
    <Button
      ref={ref}
      variant="ghost"
      size="icon"
      data-day={day.date.toLocaleDateString()}
      data-selected-single={
        modifiers["selected"] &&
        !modifiers["range_start"] &&
        !modifiers["range_end"] &&
        !modifiers["range_middle"]
      }
      data-range-start={modifiers["range_start"]}
      data-range-end={modifiers["range_end"]}
      data-range-middle={modifiers["range_middle"]}
      className={cn(
        "flex aspect-square h-auto w-full min-w-(--cell-size) flex-col gap-1 rounded-lg font-normal leading-none text-[#123b2a] transition-colors hover:bg-[#dff1e2] hover:text-[#14532d] focus-visible:ring-2 focus-visible:ring-[#16834a]/40",
        "data-[selected-single=true]:bg-[#16834a] data-[selected-single=true]:text-white data-[selected-single=true]:hover:bg-[#126b3c]",
        "data-[range-start=true]:bg-[#16834a] data-[range-start=true]:text-white",
        "data-[range-end=true]:bg-[#16834a] data-[range-end=true]:text-white",
        "data-[range-middle=true]:bg-[#dff1e2] data-[range-middle=true]:text-[#14532d]",
        "group-data-[focused=true]/day:relative group-data-[focused=true]/day:z-10",
        "[&>span]:text-xs [&>span]:opacity-70",
        defaultClassNames.day,
        className,
      )}
      {...props}
    />
  );
}

export { Calendar, CalendarDayButton };