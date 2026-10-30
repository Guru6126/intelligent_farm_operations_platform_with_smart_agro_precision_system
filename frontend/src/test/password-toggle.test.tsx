import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Input } from "@/components/ui/input";

describe("Password Input Eye Toggle", () => {
  it("renders a toggle eye button inside password inputs", () => {
    render(<Input type="password" placeholder="Enter password" data-testid="pw-input" />);

    const input = screen.getByTestId("pw-input") as HTMLInputElement;
    expect(input.type).toBe("password");

    const toggleBtn = screen.getByRole("button", { name: /show password/i });
    expect(toggleBtn).toBeDefined();

    // Toggle to visible
    fireEvent.click(toggleBtn);
    expect(input.type).toBe("text");
    expect(screen.getByRole("button", { name: /hide password/i })).toBeDefined();

    // Toggle back to hidden
    fireEvent.click(toggleBtn);
    expect(input.type).toBe("password");
    expect(screen.getByRole("button", { name: /show password/i })).toBeDefined();
  });

  it("does not render a toggle eye button for non-password inputs", () => {
    render(<Input type="email" placeholder="Enter email" />);
    expect(screen.queryByRole("button")).toBeNull();
  });
});

