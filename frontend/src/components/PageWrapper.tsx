import type { ReactNode } from "react";

type PageWrapperProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
};

export default function PageWrapper({
  eyebrow,
  title,
  description,
  actions,
  children,
}: PageWrapperProps) {
  return (
    <section className="mx-auto flex w-full max-w-[1800px] flex-col gap-6 p-4 md:p-6">
      <div className="flex flex-col gap-4 border-b border-slate-800 pb-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="min-w-0">
          {eyebrow && (
            <p className="mb-2 text-[11px] font-black uppercase tracking-[0.18em] text-cyan-400">
              {eyebrow}
            </p>
          )}
          <h2 className="text-2xl font-black tracking-tight text-white md:text-3xl">{title}</h2>
          {description && (
            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400">{description}</p>
          )}
        </div>
        {actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}
      </div>

      {children}
    </section>
  );
}
