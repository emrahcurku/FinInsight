import React from 'react';

interface Column<T> {
  header: string;
  accessor?: keyof T;
  render?: (item: T) => React.ReactNode;
  align?: 'left' | 'center' | 'right';
  width?: string;
}

interface TableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (item: T) => string;
  emptyText?: string;
}

export function Table<T>({ columns, data, keyExtractor, emptyText = 'No data available.' }: TableProps<T>) {
  if (data.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '2rem', color: '#64748b', fontSize: '0.9rem' }}>
        {emptyText}
      </div>
    );
  }

  return (
    <div style={{ width: '100%', overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.08)' }}>
            {columns.map((col, idx) => (
              <th
                key={idx}
                style={{
                  padding: '10px 14px',
                  color: '#94a3b8',
                  fontWeight: 600,
                  fontSize: '0.75rem',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  textAlign: col.align || 'left',
                  width: col.width,
                }}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((item) => (
            <tr
              key={keyExtractor(item)}
              style={{
                borderBottom: '1px solid rgba(255, 255, 255, 0.04)',
                transition: 'background-color 0.15s ease',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.02)')}
              onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
            >
              {columns.map((col, idx) => (
                <td
                  key={idx}
                  style={{
                    padding: '12px 14px',
                    color: '#f8fafc',
                    textAlign: col.align || 'left',
                  }}
                >
                  {col.render ? col.render(item) : col.accessor ? String(item[col.accessor]) : null}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
