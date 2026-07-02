import { adminReturns } from "@/lib/admin-data";
import { PageHead } from "@/components/admin/ui";
import ReturnsTable from "@/components/admin/ReturnsTable";

export default async function ReturnsPage() {
  const returns = await adminReturns();
  return (
    <div>
      <PageHead
        title="🔁 Returns & Cancellations"
        sub="Customer requests from the Track Order page — approve, arrange pickup and refund."
      />
      <ReturnsTable returns={returns} />
    </div>
  );
}
