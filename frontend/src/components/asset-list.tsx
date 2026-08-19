import type { ReactNode } from "react";

import type { Asset } from "@/types/api";

type AssetListProps = {
  assets: Asset[];
};

export function AssetList({ assets }: AssetListProps) {
  return (
    <section aria-labelledby="assets-heading">
      <div className="mb-4">
        <h2 id="assets-heading" className="text-xl font-semibold text-[#102033]">
          Varlıklar
        </h2>
        <p className="text-sm text-[#64748b]">Tanımlı varlık: {assets.length}</p>
      </div>

      {assets.length === 0 ? (
        <div className="rounded-lg border border-dashed border-[#cbd5e1] bg-white px-5 py-8 text-center text-sm text-[#64748b]">
          Henüz varlık tanımı yok.
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-[#d8dee8] bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] border-collapse text-left text-sm">
              <thead className="bg-[#f7f9fc] text-xs uppercase tracking-[0.12em] text-[#64748b]">
                <tr>
                  <TableHeader>Symbol</TableHeader>
                  <TableHeader>Name</TableHeader>
                  <TableHeader>Type</TableHeader>
                  <TableHeader>Currency</TableHeader>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e2e8f0]">
                {assets.map((asset) => (
                  <tr key={asset.id} className="hover:bg-[#fafcff]">
                    <TableCell>
                      <span className="font-semibold text-[#102033]">{asset.symbol}</span>
                    </TableCell>
                    <TableCell>{asset.name}</TableCell>
                    <TableCell>
                      <span className="rounded-md bg-[#edf2f7] px-2 py-1 text-xs font-medium text-[#334155]">
                        {asset.assetType}
                      </span>
                    </TableCell>
                    <TableCell>
                      <span className="font-medium text-[#334155]">{asset.currency}</span>
                    </TableCell>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}

function TableHeader({ children }: { children: ReactNode }) {
  return <th className="px-4 py-3 font-semibold text-left">{children}</th>;
}

function TableCell({ children }: { children: ReactNode }) {
  return <td className="whitespace-nowrap px-4 py-4 text-[#334155]">{children}</td>;
}
