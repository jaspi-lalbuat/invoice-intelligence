"use client";

import { InvoiceResponse, formatRelativeTime } from "../lib/invoice";

type TechnicalDetailsProps = {
  invoice: InvoiceResponse;
};

export default function TechnicalDetails({
  invoice,
}: TechnicalDetailsProps) {
  return (
    <div className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-5">
      <div className="text-sm font-medium text-white">
        Technical details
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 sm:grid-cols-3">
        <div>
          <div className="text-xs text-zinc-600">
            Job ID
          </div>

          <div className="mt-1 break-all font-mono text-xs text-zinc-400">
            {invoice.jobId}
          </div>
        </div>

        <div>
          <div className="text-xs text-zinc-600">
            Created
          </div>

          <div className="mt-1 text-sm text-zinc-300">
            {formatRelativeTime(invoice.createdAt)}
          </div>
        </div>

        <div>
          <div className="text-xs text-zinc-600">
            Updated
          </div>

          <div className="mt-1 text-sm text-zinc-300">
            {formatRelativeTime(invoice.updatedAt)}
          </div>
        </div>
      </div>
    </div>
  );
}