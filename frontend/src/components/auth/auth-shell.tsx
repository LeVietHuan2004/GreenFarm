import { ArrowLeft, Leaf } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";

type AuthShellProps = {
  title: string;
  description: string;
  children: ReactNode;
  footer: ReactNode;
  visualEyebrow?: string;
  visualTitle?: string;
  topAction?: ReactNode;
  headingIcon?: ReactNode;
};

export function AuthShell({
  title,
  description,
  children,
  footer,
  visualEyebrow = "GreenFarm",
  visualTitle = "Nông sản tươi, kết nối minh bạch.",
  topAction,
  headingIcon
}: AuthShellProps) {
  return (
    <main className="auth-page">
      <section className="auth-visual" aria-label="Nông sản GreenFarm">
        <Image
          src="/greenfarm-auth.png"
          alt="Rau củ tươi được thu hoạch và sắp xếp tại nông trại"
          fill
          priority
          sizes="(max-width: 800px) 100vw, 46vw"
        />
        <div className="auth-visual-caption">
          <span>{visualEyebrow}</span>
          <strong>{visualTitle}</strong>
        </div>
      </section>

      <section className="auth-content">
        <div className="auth-form-wrap">
          <div className="auth-topbar">
            <Link href="/" className="icon-link" aria-label="Về trang chính" title="Về trang chính">
              <ArrowLeft size={19} aria-hidden="true" />
            </Link>
            <div className="auth-top-actions">
              <Link href="/" className="brand-link">
                <span className="brand-mark"><Leaf size={20} aria-hidden="true" /></span>
                <span>GreenFarm</span>
              </Link>
              {topAction}
            </div>
          </div>

          <header className="auth-heading">
            {headingIcon && <span className="auth-heading-icon">{headingIcon}</span>}
            <h1>{title}</h1>
            <p>{description}</p>
          </header>

          {children}
          <div className="auth-footer">{footer}</div>
        </div>
      </section>
    </main>
  );
}
