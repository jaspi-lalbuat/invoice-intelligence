"use client";

import { InvoiceResponse } from "../lib/invoice";

type InvoiceResultProps = {
  invoice: InvoiceResponse;
};

export default function InvoiceResult({
  invoice,
}: InvoiceResultProps) {
  if (!invoice.invoice || !invoice.validation) {
    return (
      <div className="flex min-h-[350px] items-center justify-center text-center">
        <div>
          <div className="mx-auto h-5 w-5 animate-spin rounded-full border-2 border-zinc-700 border-t-zinc-300" />

          <p className="mt-4 text-sm text-zinc-500">
            {invoice.status === "QUEUED" ||
            invoice.status === "PROCESSING"
              ? "Invoice is still being processed..."
              : "Loading invoice..."}
          </p>
        </div>
      </div>
    );
  }

  const { invoice: data, validation } = invoice;

  return (
    <div className="space-y-6 pt-6">
      {/* Validation issues */}
      {validation.issues.length > 0 && (
        <div className="rounded-xl border border-amber-900/40 bg-amber-950/10 p-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-amber-300">
                Validation issues
              </p>

              <p className="mt-1 text-xs text-zinc-500">
                Review required before accepting this invoice.
              </p>
            </div>

            <span className="rounded-full border border-amber-900/50 bg-amber-950/20 px-2.5 py-1 text-[11px] text-amber-400">
              {validation.issues.length}
            </span>
          </div>

          <div className="mt-4 space-y-3">
            {validation.issues.map((issue) => (
              <div
                key={issue.code}
                className="rounded-lg border border-zinc-800 bg-zinc-950/50 p-4"
              >
                <div className="text-sm font-medium text-zinc-200">
                  {issue.code.replaceAll("_", " ")}
                </div>

                <div className="mt-1 text-sm text-zinc-400">
                  {issue.message}
                </div>

                <div className="mt-2 text-xs text-zinc-600">
                  Difference: ₹
                  {issue.difference.toLocaleString("en-IN")}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Invoice information */}
      <div className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-5">
        <div className="text-sm font-medium text-white">
          Invoice details
        </div>

        <div className="mt-5 grid grid-cols-2 gap-x-6 gap-y-5">
          <div>
            <div className="text-xs text-zinc-600">
              Invoice number
            </div>

            <div className="mt-1 text-sm text-zinc-300">
              {data.invoiceNumber}
            </div>
          </div>

          <div>
            <div className="text-xs text-zinc-600">
              Invoice date
            </div>

            <div className="mt-1 text-sm text-zinc-300">
              {data.invoiceDate}
            </div>
          </div>

          <div>
            <div className="text-xs text-zinc-600">
              Supplier
            </div>

            <div className="mt-1 text-sm text-zinc-300">
              {data.supplier.name}
            </div>
          </div>

          <div>
            <div className="text-xs text-zinc-600">
              Customer
            </div>

            <div className="mt-1 text-sm text-zinc-300">
              {data.customer.name}
            </div>
          </div>
        </div>
      </div>

      {/* Line items */}
      <div className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-5">
        <div className="text-sm font-medium text-white">
          Line items
        </div>

        <div className="mt-4 overflow-hidden rounded-lg border border-zinc-800">
          <table className="w-full text-sm">
            <thead className="bg-zinc-950/60">
              <tr className="border-b border-zinc-800 text-left text-xs text-zinc-500">
                <th className="px-4 py-3 font-medium">
                  Description
                </th>

                <th className="px-4 py-3 text-right font-medium">
                  Qty
                </th>

                <th className="px-4 py-3 text-right font-medium">
                  Unit price
                </th>

                <th className="px-4 py-3 text-right font-medium">
                  Calculated
                </th>
              </tr>
            </thead>

            <tbody>
              {data.lineItems.map((item, index) => (
                <tr
                  key={`${item.description}-${index}`}
                  className="border-b border-zinc-800 last:border-0"
                >
                  <td className="px-4 py-3 text-zinc-200">
                    {item.description}
                  </td>

                  <td className="px-4 py-3 text-right text-zinc-400">
                    {item.quantity}
                  </td>

                  <td className="px-4 py-3 text-right text-zinc-400">
                    ₹
                    {item.unitPrice.toLocaleString("en-IN")}
                  </td>

                  <td className="px-4 py-3 text-right text-zinc-200">
                    ₹
                    {(
                      item.quantity * item.unitPrice
                    ).toLocaleString("en-IN")}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Totals */}
      <div className="rounded-xl border border-zinc-800 bg-zinc-900/60 p-5">
        <div className="text-sm font-medium text-white">
          Invoice totals
        </div>

        <div className="mt-4 max-w-md space-y-3 text-sm">
          <div className="flex justify-between">
            <span className="text-zinc-500">
              Stated subtotal
            </span>

            <span className="text-zinc-300">
              ₹
              {data.subtotal.toLocaleString("en-IN")}
            </span>
          </div>

          <div className="flex justify-between">
            <span className="text-zinc-500">
              Calculated subtotal
            </span>

            <span className="text-zinc-300">
              ₹
              {validation.calculatedSubtotal.toLocaleString(
                "en-IN",
              )}
            </span>
          </div>

          <div className="border-t border-zinc-800 pt-3">
            <div className="flex justify-between">
              <span className="text-zinc-500">
                Stated total
              </span>

              <span className="text-zinc-300">
                ₹
                {data.total.toLocaleString("en-IN")}
              </span>
            </div>

            <div className="mt-2 flex justify-between">
              <span className="text-zinc-500">
                Calculated total
              </span>

              <span className="text-zinc-300">
                ₹
                {validation.calculatedTotal.toLocaleString(
                  "en-IN",
                )}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}