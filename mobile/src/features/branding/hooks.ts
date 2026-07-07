import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";

/** Mirrors BrandingDto (dto/admin/BrandingDto.java). */
export interface Branding {
  logoUrl?: string;
  siteName?: string;
}

/** Public branding — logo URL + site name (GET /api/v1/branding). */
export function useBranding() {
  return useQuery({
    queryKey: ["branding"],
    queryFn: () => apiGet<Branding>("/api/v1/branding"),
  });
}
