"use client";

import {
  InvoiceSummary,
  formatRelativeTime,
  formatStatus,
  getStatusColor,
} from "../lib/invoice";

type InvoiceListProps = {
  invoices: InvoiceSummary[];
  selectedJobId: string | null;
  onSelect: (jobId: string) => void;
};

export default function InvoiceList({
  invoices,
  selectedJobId,
  onSelect,
}: InvoiceListProps) {
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3">
      <div className="px-2 pb-3 pt-2">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-medium text-white">
            Invoices
          </h3>

          <span className="text-xs text-zinc-600">
            {invoices.length}
          </span>
        </div>
      </div>

      {invoices.length === 0 ? (
        <div className="px-2 py-8 text-center">
          <p className="text-xs text-zinc-600">
            No invoices yet
          </p>
        </div>
      ) : (
        <div className="space-y-1">
          {invoices.map((invoice) => {
            const isSelected =
              invoice.jobId === selectedJobId;

            return (
              <button
                key={invoice.jobId}
                type="button"
                onClick={() => onSelect(invoice.jobId)}
                className={`w-full rounded-xl p-3 text-left transition ${
                  isSelected
                    ? "bg-zinc-800/80 ring-1 ring-zinc-700"
                    : "hover:bg-zinc-800/50"
                }`}
              >
                <div className="flex items-start gap-3">
                  <span
                    className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${getStatusColor(
                      invoice.status,
                    )}`}
                  />

                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-zinc-200">
                      {invoice.originalFileName}
                    </div>

                    <div className="mt-1 flex items-center justify-between gap-2">
                      <span className="truncate text-xs text-zinc-500">
                        {formatStatus(invoice.status)}
                      </span>

                      <span className="shrink-0 text-[11px] text-zinc-600">
                        {formatRelativeTime(
                          invoice.createdAt,
                        )}
                      </span>
                    </div>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}