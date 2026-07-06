import { QueryClient } from "@tanstack/react-query";

/** Centralised cache. staleTime keeps lists snappy; retry is low so failures
 *  surface quickly. For offline caching, wrap with a persister
 *  (@tanstack/query-async-storage-persister) — see MOBILE_APP.md. */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: false,
    },
    mutations: { retry: 0 },
  },
});
