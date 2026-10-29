/** Keep the selected ward in the delivery snapshot used by both carriers. */
export function addressWithWard(address: string, wardName?: string | null): string {
  const value = address.trim();
  const ward = wardName?.trim();
  if (!ward || value.split(",").some(part => part.trim().toLocaleLowerCase("vi-VN") === ward.toLocaleLowerCase("vi-VN"))) return value;
  return `${value}, ${ward}`;
}

export function addressWithoutTrailingWard(address: string, wardName?: string | null): string {
  const parts = address.split(",");
  if (wardName && parts.at(-1)?.trim().toLocaleLowerCase("vi-VN") === wardName.trim().toLocaleLowerCase("vi-VN")) {
    return parts.slice(0, -1).join(",").trim();
  }
  return address;
}
