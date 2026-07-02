export default function LegalLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="legal max-w-[820px] mx-auto px-5 py-10 animate-fade">
      {children}
    </div>
  );
}
