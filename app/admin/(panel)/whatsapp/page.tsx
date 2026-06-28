import { PageHead } from "@/components/admin/ui";
import Broadcast from "@/components/admin/Broadcast";

export default function WhatsAppPage() {
  return (
    <div>
      <PageHead title="💬 WhatsApp Offers" sub="Broadcast offers to customers via your Meta WhatsApp Business account." />
      <Broadcast />
    </div>
  );
}
