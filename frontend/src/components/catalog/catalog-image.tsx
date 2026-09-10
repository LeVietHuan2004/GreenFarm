"use client";

import { ImageOff } from "lucide-react";
import { useState } from "react";

type CatalogImageProps = {
  src: string | null;
  fallbackSrc?: string | null;
  alt: string;
  className?: string;
};

function resolveImageUrl(src: string | null | undefined) {
  if (!src) return null;
  if (/^https?:\/\//i.test(src)) return src;
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
  const origin = apiBase.replace(/\/api\/?$/, "");
  return `${origin}/${src.replace(/^\//, "")}`;
}

export function CatalogImage({ src, fallbackSrc, alt, className }: CatalogImageProps) {
  const primary = resolveImageUrl(src);
  const fallback = resolveImageUrl(fallbackSrc);
  const [failedSources, setFailedSources] = useState<string[]>([]);
  const currentSrc = [primary, fallback].find(
    (candidate): candidate is string => Boolean(candidate && !failedSources.includes(candidate))
  ) ?? null;

  if (!currentSrc) {
    return (
      <div className={`catalog-image-placeholder ${className ?? ""}`} role="img" aria-label={alt}>
        <ImageOff size={28} aria-hidden="true" />
      </div>
    );
  }

  return (
    // Product image URLs are managed by administrators and can come from multiple CDNs.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={currentSrc}
      alt={alt}
      className={className}
      onError={() => setFailedSources((sources) => [...sources, currentSrc])}
    />
  );
}
