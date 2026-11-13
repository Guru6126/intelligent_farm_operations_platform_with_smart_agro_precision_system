export function isValidPhone(phone: string): boolean {
  // Allow only digits, exactly 10 characters
  return /^\d{10}$/.test(phone.trim());
}

export function isValidEmail(email: string): boolean {
  // Simple email regex
  return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim());
}

export function isValidPassword(pw: string): boolean {
  // Minimum 8 chars, at least one lowercase, one uppercase, one digit, one special char
  return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\[\]{};:'",.<>/?\\|`~-]).{8,}$/.test(pw);
}

export function validateValues(values: Record<string, unknown>): string | null {
  // Phone validation
  if (typeof values.phone === "string" && values.phone) {
    if (!isValidPhone(values.phone)) return "Phone number must be exactly 10 digits.";
  }
  // Email validation
  if (typeof values.email === "string" && values.email) {
    if (!isValidEmail(values.email)) return "Invalid email address.";
  }
  // Password validation
  if (typeof values.password === "string" && values.password) {
    if (!isValidPassword(values.password))
      return "Password must be at least 8 characters, include lowercase, uppercase, number, and special character.";
  }
  return null;
}

