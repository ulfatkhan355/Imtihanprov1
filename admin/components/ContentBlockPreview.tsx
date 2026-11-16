import React from 'react';
import { BookMarked, CheckCircle2, Image as ImageIcon, Lightbulb } from 'lucide-react';
import type { EducationalContentBlock } from '../../shared-contracts/types';

export function ContentBlockPreview({ blocks }: { blocks: EducationalContentBlock[] }) {
  if (blocks.length === 0) {
    return (
      <div className="rounded-xl border border-[#CBD8EE] bg-white p-8 text-center text-sm text-[#475569] dark:border-[#223B70] dark:bg-[#0D1B38] dark:text-[#94A8CE]">
        No structured content blocks added yet. Add a block from the toolbar to preview the student reading experience.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {blocks.map((block) => {
        switch (block.type) {
          case 'heading':
            return (
              <div key={block.id} className="pt-1">
                {block.sectionCode && (
                  <div className="text-xs font-mono text-[#1D4ED8] dark:text-[#60A5FA]">
                    Section {block.sectionCode}
                  </div>
                )}
                <h3 className="font-display text-lg font-bold text-[#0F172A] dark:text-white">
                  {block.text}
                </h3>
              </div>
            );
          case 'subheading':
            return (
              <h4
                key={block.id}
                className="font-display text-base font-semibold text-[#1E293B] dark:text-[#DCE8FC]"
              >
                {block.text}
              </h4>
            );
          case 'paragraph':
            return (
              <p
                key={block.id}
                className="text-sm leading-relaxed text-[#334155] dark:text-[#C5D5F2]"
              >
                {block.text}
              </p>
            );
          case 'math_expression':
            return (
              <div
                key={block.id}
                className="rounded-xl border border-[#CBD8EE] bg-[#F8FAFE] p-4 dark:border-[#24407A] dark:bg-[#102145]"
              >
                <div className="font-mono text-sm font-semibold text-[#0F172A] dark:text-[#93C5FD]">
                  {block.plainNotation}
                </div>
                <div className="mt-1 font-mono text-[11px] text-[#64748B] dark:text-[#7A93C2]">
                  LaTeX: {block.latex}
                </div>
                {block.caption && (
                  <div className="mt-1.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                    {block.caption}
                  </div>
                )}
              </div>
            );
          case 'definition':
            return (
              <div
                key={block.id}
                className="rounded-xl border border-[#CBD8EE] bg-[#F8FAFE] p-4 dark:border-[#24407A] dark:bg-[#102145]"
              >
                <div className="flex items-center gap-2 text-xs font-semibold text-[#1D4ED8] dark:text-[#60A5FA]">
                  <BookMarked className="h-3.5 w-3.5" />
                  <span>Important Definition · {block.term}</span>
                </div>
                <p className="mt-1.5 text-sm text-[#1E293B] dark:text-[#E2ECFC]">
                  {block.definition}
                </p>
                {block.formulaHint && (
                  <div className="mt-2 font-mono text-xs text-[#0F172A] dark:text-[#93C5FD]">
                    Formula: {block.formulaHint}
                  </div>
                )}
              </div>
            );
          case 'exam_point':
            return (
              <div
                key={block.id}
                className="rounded-xl border border-[#FCD34D]/60 bg-[#FFFBEB] p-4 dark:border-[#B45309]/60 dark:bg-[#2B210B]"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="flex items-center gap-2 text-xs font-semibold text-[#92400E] dark:text-[#FCD34D]">
                    <Lightbulb className="h-4 w-4" />
                    {block.title}
                  </span>
                  {block.boardFrequency && (
                    <span className="text-[11px] font-medium text-[#92400E] dark:text-[#FDE68A]">
                      {block.boardFrequency} Yield
                    </span>
                  )}
                </div>
                <ul className="mt-2 list-disc pl-5 space-y-1 text-xs text-[#78350F] dark:text-[#FEF3C7]">
                  {block.points.map((pt, idx) => (
                    <li key={idx}>{pt}</li>
                  ))}
                </ul>
              </div>
            );
          case 'table':
            return (
              <div
                key={block.id}
                className="overflow-x-auto rounded-xl border border-[#CBD8EE] dark:border-[#24407A]"
              >
                {block.title && (
                  <div className="px-4 py-2.5 text-xs font-semibold border-b border-[#CBD8EE] bg-[#F8FAFE] text-[#0F172A] dark:border-[#24407A] dark:bg-[#102145] dark:text-white">
                    {block.title}
                  </div>
                )}
                <table className="w-full text-left text-xs">
                  <thead className="bg-[#F1F5F9] text-[#334155] dark:bg-[#0B1733] dark:text-[#94A8CE]">
                    <tr>
                      {block.headers.map((h, i) => (
                        <th key={i} className="px-3.5 py-2 font-semibold">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#E2E8F0] dark:divide-[#1C3363]">
                    {block.rows.map((row, rIdx) => (
                      <tr key={rIdx} className="bg-white dark:bg-[#0D1B38]">
                        {row.map((cell, cIdx) => (
                          <td
                            key={cIdx}
                            className="px-3.5 py-2 text-[#1E293B] dark:text-[#DCE8FC]"
                          >
                            {cell}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            );
          case 'list':
            return (
              <div key={block.id} className="space-y-1.5">
                {block.title && (
                  <div className="text-xs font-semibold text-[#0F172A] dark:text-white">
                    {block.title}
                  </div>
                )}
                {block.ordered ? (
                  <ol className="list-decimal pl-5 space-y-1 text-sm text-[#334155] dark:text-[#C5D5F2]">
                    {block.items.map((it, i) => (
                      <li key={i}>{it}</li>
                    ))}
                  </ol>
                ) : (
                  <ul className="list-disc pl-5 space-y-1 text-sm text-[#334155] dark:text-[#C5D5F2]">
                    {block.items.map((it, i) => (
                      <li key={i}>{it}</li>
                    ))}
                  </ul>
                )}
              </div>
            );
          case 'example':
            return (
              <div
                key={block.id}
                className="rounded-xl border border-[#CBD8EE] bg-white p-4 dark:border-[#24407A] dark:bg-[#0D1B38]"
              >
                <div className="text-xs font-semibold text-[#1D4ED8] dark:text-[#60A5FA]">
                  {block.title}
                </div>
                <div className="mt-1 text-sm font-medium text-[#0F172A] dark:text-white">
                  {block.problemStatement}
                </div>
                <ol className="mt-2 list-decimal pl-5 space-y-1 text-xs text-[#475569] dark:text-[#B5C6E6]">
                  {block.steps.map((s, i) => (
                    <li key={i}>{s}</li>
                  ))}
                </ol>
                <div className="mt-2 text-xs font-semibold text-[#15803D] dark:text-[#4ADE80]">
                  Answer: {block.finalAnswer}
                </div>
              </div>
            );
          case 'diagram':
            return (
              <div
                key={block.id}
                className="rounded-xl border border-[#CBD8EE] bg-[#F8FAFE] p-4 dark:border-[#24407A] dark:bg-[#102145]"
              >
                <div className="flex items-center gap-2 text-xs font-semibold text-[#0F172A] dark:text-white">
                  <ImageIcon className="h-4 w-4 text-[#1D4ED8] dark:text-[#60A5FA]" />
                  <span>Diagram Asset · {block.title}</span>
                </div>
                <div className="mt-2 rounded-lg border border-dashed border-[#94A3B8] dark:border-[#365594] p-4 text-center text-xs text-[#475569] dark:text-[#94A8CE]">
                  Asset Key: <span className="font-mono">{block.assetKey}</span> ({block.aspectRatio})
                  <div className="mt-1 text-[11px]">{block.altText}</div>
                </div>
                <div className="mt-1.5 text-xs text-[#475569] dark:text-[#94A8CE]">
                  {block.caption}
                </div>
              </div>
            );
          case 'mcq':
            return (
              <div
                key={block.id}
                className="rounded-xl border border-[#CBD8EE] bg-white p-4 dark:border-[#24407A] dark:bg-[#0D1B38]"
              >
                <div className="text-xs font-semibold text-[#1D4ED8] dark:text-[#60A5FA]">
                  Inline Practice MCQ · {block.difficulty}
                </div>
                <div className="mt-1 text-sm font-medium text-[#0F172A] dark:text-white">
                  {block.question}
                </div>
                <div className="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {block.options.map((opt) => {
                    const isCorrect = opt.id === block.correctOptionId;
                    return (
                      <div
                        key={opt.id}
                        className={`rounded-lg border px-3 py-2 text-xs flex items-center justify-between ${
                          isCorrect
                            ? 'border-[#16A34A]/60 bg-[#DCFCE7]/50 text-[#14532D] dark:border-[#22C55E]/50 dark:bg-[#14532D]/30 dark:text-[#BBF7D0]'
                            : 'border-[#E2E8F0] text-[#334155] dark:border-[#1E3566] dark:text-[#C5D5F2]'
                        }`}
                      >
                        <span>
                          <strong className="mr-1.5">{opt.id}.</strong>
                          {opt.text}
                        </span>
                        {isCorrect && <CheckCircle2 className="h-3.5 w-3.5 shrink-0" />}
                      </div>
                    );
                  })}
                </div>
                <div className="mt-2 text-xs text-[#475569] dark:text-[#94A8CE]">
                  Explanation: {block.explanation}
                </div>
              </div>
            );
          case 'solved_question':
            return (
              <div
                key={block.id}
                className="rounded-xl border border-[#CBD8EE] bg-white p-4 dark:border-[#24407A] dark:bg-[#0D1B38]"
              >
                <div className="flex items-center justify-between text-xs text-[#475569] dark:text-[#94A8CE]">
                  <span className="font-semibold text-[#1D4ED8] dark:text-[#60A5FA]">
                    Solved {block.questionType} question · {block.marks} marks
                  </span>
                  {block.yearReference && <span>{block.yearReference}</span>}
                </div>
                <div className="mt-1 text-sm font-medium text-[#0F172A] dark:text-white">
                  {block.question}
                </div>
                <ul className="mt-2 list-disc pl-5 space-y-1 text-xs text-[#334155] dark:text-[#C5D5F2]">
                  {block.answerSteps.map((step, i) => (
                    <li key={i}>{step}</li>
                  ))}
                </ul>
              </div>
            );
        }
      })}
    </div>
  );
}
