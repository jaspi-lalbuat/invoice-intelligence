"use client";

import { ChangeEvent, useEffect, useRef, useState } from "react";

const API_BASE_URL = "http://localhost:8080";

type ProcessingStatus =
  | "QUEUED"
  | "PROCESSING"
  | "READY"
  | "REVIEW_REQUIRED"
  | "FAILED";

type InvoiceResponse = {
  jobId: string;
  status: ProcessingStatus;
  invoice: {
    invoiceNumber: string;
    invoiceDate: string;
    dueDate: string;
    currency: string;
    supplier: {
      name: string;
      address: string | null;
      taxId: string | null;
    };
    customer: {
      name: string;
      address: string | null;
      taxId: string | null;
    };
    lineItems: {
      description: string;
      quantity: number;
      unitPrice: number;
      taxRate: number | null;
      statedTotal: number | null;
    }[];
    subtotal: number;
    taxes: {
      cgst: number | null;
      sgst: number | null;
      igst: number | null;
    };
    discount: number | null;
    total: number;
  };
  validation: {
    status: string;
    calculatedSubtotal: number;
    subtotalDifference: number;
    calculatedTotal: number;
    totalDifference: number;
    issues: {
      code: string;
      message: string;
      difference: number;
    }[];
  };
  createdAt: string;
  updatedAt: string;
};

type InvoiceSummary = {
  jobId: string;
  originalFileName: string;
  status: ProcessingStatus;
  createdAt: string;
  updatedAt: string;
};

const isTerminalStatus = (status: ProcessingStatus) =>
  status === "READY" ||
  status === "REVIEW_REQUIRED" ||
  status === "FAILED";

const formatStatus = (status: ProcessingStatus) =>
  status.replaceAll("_", " ");

const getStatusColor = (status: ProcessingStatus) => {
  switch (status) {
    case "READY":
      return "bg-emerald-400";

    case "REVIEW_REQUIRED":
      return "bg-amber-400";

    case "FAILED":
      return "bg-red-400";

    case "PROCESSING":
      return "bg-blue-400";

    case "QUEUED":
    default:
      return "bg-zinc-500";
  }
};

const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();

  const seconds = Math.floor(
    (now.getTime() - date.getTime()) / 1000,
  );

  if (seconds < 60) {
    return "just now";
  }

  const minutes = Math.floor(seconds / 60);

  if (minutes < 60) {
    return `${minutes}m ago`;
  }

  const hours = Math.floor(minutes / 60);

  if (hours < 24) {
    return `${hours}h ago`;
  }

  const days = Math.floor(hours / 24);

  return `${days}d ago`;
};

export default function Home() {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [uploading, setUploading] = useState(false);

  const [invoices, setInvoices] = useState<InvoiceSummary[]>([]);

  const [selectedJobId, setSelectedJobId] =
    useState<string | null>(null);

  const [selectedInvoice, setSelectedInvoice] =
    useState<InvoiceResponse | null>(null);

  const [statusHistory, setStatusHistory] = useState<
    ProcessingStatus[]
  >([]);

  const [error, setError] = useState<string | null>(null);

  const [refreshing, setRefreshing] = useState(false);

  /*
   * Update the status of an invoice in the left-hand list.
   */
  const updateInvoiceInList = (
    jobId: string,
    status: ProcessingStatus,
  ) => {
    setInvoices((previous) =>
      previous.map((invoice) =>
        invoice.jobId === jobId
          ? {
              ...invoice,
              status,
              updatedAt: new Date().toISOString(),
            }
          : invoice,
      ),
    );
  };

  /*
   * Load all previously uploaded invoices.
   *
   * The backend already returns them newest-first.
   */
  const loadInvoices = async () => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/documents`,
      );

      if (!response.ok) {
        throw new Error(
          `Failed to load invoices (${response.status})`,
        );
      }

      const data: InvoiceSummary[] = await response.json();

      setInvoices(data);

      /*
       * Automatically select the newest invoice when
       * there isn't already a selection.
       */
      if (data.length > 0) {
        setSelectedJobId((current) => current ?? data[0].jobId);
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to load invoices.",
      );
    }
  };

  /*
   * Load existing invoices when the page opens.
   */
  useEffect(() => {
    loadInvoices();
  }, []);

  /*
   * Fetch the complete details of the selected invoice.
   *
   * This is separate from the list API because the list endpoint
   * intentionally returns lightweight summaries.
   */
  useEffect(() => {
    if (!selectedJobId) {
      setSelectedInvoice(null);
      return;
    }

    let cancelled = false;

    const loadSelectedInvoice = async () => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/api/v1/documents/${selectedJobId}`,
        );

        if (!response.ok) {
          throw new Error(
            `Failed to fetch invoice (${response.status})`,
          );
        }

        const data: InvoiceResponse = await response.json();

        if (cancelled) {
          return;
        }

        setSelectedInvoice(data);

        /*
         * An existing invoice only gives us its current state.
         *
         * Therefore, if it was already completed before this
         * browser session, we cannot reconstruct its historical
         * QUEUED → PROCESSING transitions.
         */
        setStatusHistory([data.status]);
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : "Failed to load invoice.",
          );
        }
      }
    };

    loadSelectedInvoice();

    return () => {
      cancelled = true;
    };
  }, [selectedJobId]);

  /*
   * Poll the currently selected invoice while it is processing.
   */
    useEffect(() => {
    if (!selectedJobId) {
      return;
    }

    let cancelled = false;
    let timeoutId: NodeJS.Timeout | null = null;

    const poll = async () => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/api/v1/documents/${selectedJobId}`,
          { cache: "no-store" }
        );

        if (!response.ok) {
          throw new Error("Failed to fetch invoice status");
        }

        const data = await response.json();

        if (cancelled) {
          return;
        }

        setSelectedInvoice(data);

        setStatusHistory((previous) => {
          if (
            previous.length > 0 &&
            previous[previous.length - 1] === data.status
          ) {
            return previous;
          }

          return [...previous, data.status];
        });

        // Stop polling once processing has finished.
        if (
          data.status === "READY" ||
          data.status === "REVIEW_REQUIRED" ||
          data.status === "FAILED"
        ) {
          return;
        }

        // Continue polling while QUEUED / PROCESSING.
        timeoutId = setTimeout(poll, 2000);
      } catch (error) {
        if (cancelled) {
          return;
        }

        console.error("Status polling failed:", error);

        // Keep polling — a transient network error shouldn't stop
        // the UI from eventually seeing the result.
        timeoutId = setTimeout(poll, 2000);
      }
    };

    poll();

    return () => {
      cancelled = true;

      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [selectedJobId]);

  /*
   * Manually refresh the selected invoice.
   */
  const refreshStatus = async () => {
    if (!selectedJobId) {
      return;
    }

    setRefreshing(true);
    setError(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/documents/${selectedJobId}`,
      );

      if (!response.ok) {
        throw new Error(
          `Failed to fetch job status (${response.status})`,
        );
      }

      const data: InvoiceResponse = await response.json();

      setSelectedInvoice(data);

      updateInvoiceInList(
        data.jobId,
        data.status,
      );

      setStatusHistory((previous) => {
        if (previous.includes(data.status)) {
          return previous;
        }

        return [...previous, data.status];
      });
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to refresh processing status.",
      );
    } finally {
      setRefreshing(false);
    }
  };

  /*
   * File selection.
   */
  const handleFileChange = (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];

    setError(null);

    if (!file) {
      setSelectedFile(null);
      return;
    }

    if (file.type !== "application/pdf") {
      setSelectedFile(null);
      setError("Please select a PDF file.");
      return;
    }

    setSelectedFile(file);
  };

  /*
   * Upload a new invoice.
   */
  const handleUpload = async () => {
    if (!selectedFile) {
      setError("Please select a PDF file first.");
      return;
    }

    setUploading(true);
    setError(null);

    const formData = new FormData();

    formData.append("file", selectedFile);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/v1/documents`,
        {
          method: "POST",
          body: formData,
        },
      );

      if (!response.ok) {
        throw new Error(
          `Upload failed (${response.status})`,
        );
      }

      const data: { jobId: string } =
        await response.json();

      /*
       * Put the new job at the top immediately.
       *
       * We don't wait for the list API to update.
       */
      const newInvoice: InvoiceSummary = {
        jobId: data.jobId,
        originalFileName:
          selectedFile.name,
        status: "QUEUED",
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      setInvoices((previous) => [
        newInvoice,
        ...previous.filter(
          (invoice) => invoice.jobId !== data.jobId,
        ),
      ]);

      /*
       * Selecting this job causes the detail panel to fetch
       * the newly created job and begin polling it.
       */
      setSelectedJobId(data.jobId);

      setSelectedFile(null);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Something went wrong while uploading.",
      );
    } finally {
      setUploading(false);
    }
  };

  const selectedSummary = invoices.find(
    (invoice) => invoice.jobId === selectedJobId,
  );

  return (
    <main className="min-h-screen bg-zinc-950 text-zinc-100">
      <div className="mx-auto flex min-h-screen max-w-7xl flex-col px-6 py-8">

        {/* Header */}
        <header className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold tracking-tight">
              Invoice Intelligence
            </h1>

            <p className="mt-1 text-sm text-zinc-400">
              AI-powered invoice extraction & validation
            </p>
          </div>

          <div className="rounded-full border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-xs text-zinc-400">
            Local environment
          </div>
        </header>

        {/* Hero */}
        <section className="py-12">
          <div>
            <p className="mb-3 text-sm font-medium text-zinc-500">
              INVOICE PROCESSING
            </p>

            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
              Turn invoices into structured data.
            </h2>

            <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-400">
              Upload a PDF and let the processing pipeline
              extract, interpret, and validate the invoice
              automatically.
            </p>
          </div>
        </section>

        {/* Main workspace */}
        <section className="grid flex-1 gap-5 lg:grid-cols-[280px_minmax(0,1fr)]">

          {/* Left column */}
          <aside className="space-y-5">

            {/* Upload */}
            <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5">
              <div>
                <h3 className="text-sm font-medium text-white">
                  Upload invoice
                </h3>

                <p className="mt-1 text-xs leading-5 text-zinc-500">
                  PDF documents only
                </p>
              </div>

              <input
                ref={fileInputRef}
                type="file"
                accept="application/pdf"
                className="hidden"
                onChange={handleFileChange}
              />

              <button
                type="button"
                onClick={() =>
                  fileInputRef.current?.click()
                }
                className="mt-5 w-full rounded-xl border border-dashed border-zinc-700 bg-zinc-950/60 px-4 py-7 text-center transition hover:border-zinc-500 hover:bg-zinc-900"
              >
                <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-lg border border-zinc-800 bg-zinc-900">
                  <svg
                    className="h-5 w-5 text-zinc-400"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={1.5}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M12 16V4m0 0 4 4m-4-4L8 8m9 4v5a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-5"
                    />
                  </svg>
                </div>

                <p className="mt-3 truncate text-sm font-medium text-zinc-300">
                  {selectedFile
                    ? selectedFile.name
                    : "Choose a PDF"}
                </p>

                <p className="mt-1 text-xs text-zinc-600">
                  {selectedFile
                    ? `${(
                        selectedFile.size /
                        1024 /
                        1024
                      ).toFixed(2)} MB`
                    : "Click to browse"}
                </p>
              </button>

              {selectedFile && (
                <button
                  type="button"
                  disabled={uploading}
                  onClick={handleUpload}
                  className="mt-3 w-full rounded-xl bg-white px-4 py-2.5 text-sm font-medium text-zinc-950 transition hover:bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {uploading
                    ? "Uploading..."
                    : "Process invoice"}
                </button>
              )}
            </div>

            {/* Invoice list */}
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
                        onClick={() => {
                          setError(null);
                          setSelectedJobId(
                            invoice.jobId,
                          );
                        }}
                        className={`w-full rounded-xl p-3 text-left transition ${
                          isSelected
                            ? "bg-zinc-800"
                            : "hover:bg-zinc-800/60"
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
                                {formatStatus(
                                  invoice.status,
                                )}
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
          </aside>

          {/* Right column */}
          <div className="min-w-0 rounded-2xl border border-zinc-800 bg-zinc-900/40">

            {!selectedJobId ? (
              <div className="flex min-h-[500px] items-center justify-center p-10 text-center">
                <div>
                  <p className="text-sm font-medium text-zinc-400">
                    Select an invoice
                  </p>

                  <p className="mt-2 text-xs text-zinc-600">
                    Upload an invoice or select one from the list.
                  </p>
                </div>
              </div>
            ) : (
              <div className="p-6">

                {/* Selected invoice header */}
                <div className="flex flex-col gap-4 border-b border-zinc-800 pb-6 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0">
                    <p className="text-xs font-medium uppercase tracking-wider text-zinc-600">
                      Selected invoice
                    </p>

                    <h3 className="mt-2 truncate text-xl font-semibold text-white">
                      {selectedSummary?.originalFileName ??
                        "Invoice"}
                    </h3>

                    {selectedInvoice?.invoice
                      ?.invoiceNumber && (
                      <p className="mt-1 text-sm text-zinc-500">
                        {
                          selectedInvoice.invoice
                            .invoiceNumber
                        }
                      </p>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={refreshStatus}
                    disabled={refreshing}
                    className="shrink-0 rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-xs font-medium text-zinc-300 transition hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {refreshing
                      ? "Refreshing..."
                      : "Refresh status"}
                  </button>
                </div>

                {/* Status */}
                <div className="border-b border-zinc-800 py-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-xs text-zinc-600">
                        Processing status
                      </p>

                      <div className="mt-2 flex items-center gap-2">
                        <span
                          className={`h-2.5 w-2.5 rounded-full ${
                            selectedInvoice
                              ? getStatusColor(
                                  selectedInvoice.status,
                                )
                              : "bg-zinc-500"
                          }`}
                        />

                        <span className="text-sm font-medium text-zinc-200">
                          {selectedInvoice
                            ? formatStatus(
                                selectedInvoice.status,
                              )
                            : "Loading..."}
                        </span>
                      </div>
                    </div>

                    {selectedInvoice && (
                      <div className="text-right">
                        <p className="text-xs text-zinc-600">
                          Job ID
                        </p>

                        <p className="mt-1 max-w-[220px] truncate font-mono text-[11px] text-zinc-500">
                          {selectedInvoice.jobId}
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Status history */}
                  {statusHistory.length > 0 && (
                    <div className="mt-5 flex flex-wrap items-center gap-2">
                      {statusHistory.map(
                        (step, index) => (
                          <div
                            key={`${step}-${index}`}
                            className="flex items-center gap-2"
                          >
                            <div
                              className={`flex items-center gap-2 rounded-full border px-3 py-1.5 ${
                                index ===
                                statusHistory.length - 1
                                  ? "border-zinc-600 bg-zinc-800"
                                  : "border-zinc-800 bg-zinc-950/60"
                              }`}
                            >
                              <span
                                className={`h-2 w-2 rounded-full ${getStatusColor(
                                  step,
                                )}`}
                              />

                              <span className="text-xs font-medium text-zinc-400">
                                {formatStatus(step)}
                              </span>
                            </div>

                            {index <
                              statusHistory.length - 1 && (
                              <span className="text-zinc-700">
                                →
                              </span>
                            )}
                          </div>
                        ),
                      )}
                    </div>
                  )}
                </div>

                {selectedInvoice &&
                  selectedInvoice.invoice &&
                  selectedInvoice.validation ? (
                    <div className="space-y-6 pt-6">

                      {/* Validation issues */}
                      {selectedInvoice.validation.issues.length > 0 && (
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
                              {selectedInvoice.validation.issues.length}
                            </span>
                          </div>

                          <div className="mt-4 space-y-3">
                            {selectedInvoice.validation.issues.map((issue) => (
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
                              {selectedInvoice.invoice.invoiceNumber}
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-zinc-600">
                              Invoice date
                            </div>

                            <div className="mt-1 text-sm text-zinc-300">
                              {selectedInvoice.invoice.invoiceDate}
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-zinc-600">
                              Supplier
                            </div>

                            <div className="mt-1 text-sm text-zinc-300">
                              {selectedInvoice.invoice.supplier.name}
                            </div>
                          </div>

                          <div>
                            <div className="text-xs text-zinc-600">
                              Customer
                            </div>

                            <div className="mt-1 text-sm text-zinc-300">
                              {selectedInvoice.invoice.customer.name}
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
                              {selectedInvoice.invoice.lineItems.map(
                                (item, index) => (
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
                                ),
                              )}
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
                              {selectedInvoice.invoice.subtotal.toLocaleString(
                                "en-IN",
                              )}
                            </span>
                          </div>

                          <div className="flex justify-between">
                            <span className="text-zinc-500">
                              Calculated subtotal
                            </span>

                            <span className="text-zinc-300">
                              ₹
                              {selectedInvoice.validation.calculatedSubtotal.toLocaleString(
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
                                {selectedInvoice.invoice.total.toLocaleString(
                                  "en-IN",
                                )}
                              </span>
                            </div>

                            <div className="mt-2 flex justify-between">
                              <span className="text-zinc-500">
                                Calculated total
                              </span>

                              <span className="text-zinc-300">
                                ₹
                                {selectedInvoice.validation.calculatedTotal.toLocaleString(
                                  "en-IN",
                                )}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>

                    </div>
                  ) : (
                    <div className="flex min-h-[350px] items-center justify-center text-center">
                      <div>
                        <div className="mx-auto h-5 w-5 animate-spin rounded-full border-2 border-zinc-700 border-t-zinc-300" />

                        <p className="mt-4 text-sm text-zinc-500">
                          {selectedInvoice?.status === "QUEUED" ||
                          selectedInvoice?.status === "PROCESSING"
                            ? "Invoice is still being processed..."
                            : "Loading invoice..."}
                        </p>
                      </div>
                    </div>
                  )}
              </div>
            )}
          </div>
        </section>

        {/* Error */}
        {error && (
          <div className="mt-5 rounded-xl border border-red-900/50 bg-red-950/20 px-4 py-3 text-sm text-red-400">
            {error}
          </div>
        )}

        {/* Pipeline */}
        <div className="mt-8 grid grid-cols-3 gap-3">
          {[
            ["01", "Extract", "PDF / OCR"],
            ["02", "Interpret", "LLM"],
            ["03", "Validate", "Deterministic"],
          ].map(([number, title, description]) => (
            <div
              key={number}
              className="rounded-xl border border-zinc-800 bg-zinc-900/40 p-4"
            >
              <p className="text-xs text-zinc-600">
                {number}
              </p>

              <p className="mt-2 text-sm font-medium">
                {title}
              </p>

              <p className="mt-1 text-xs text-zinc-500">
                {description}
              </p>
            </div>
          ))}
        </div>

        {/* Footer */}
        <footer className="mt-8 flex items-center justify-between border-t border-zinc-900 pt-5 text-xs text-zinc-600">
          <span>Invoice Intelligence</span>

          <span>
            Spring Boot · PostgreSQL · Kafka · Ollama
          </span>
        </footer>
      </div>
    </main>
  );
}