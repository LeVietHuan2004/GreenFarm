import { CatalogLanding } from "@/components/catalog/catalog-landing";
import { SiteHeader } from "@/components/layout/site-header";

export default function HomePage() {
  return (
    <div className="app-shell market-home">
      <SiteHeader />
      <CatalogLanding />
    </div>
  );
}
