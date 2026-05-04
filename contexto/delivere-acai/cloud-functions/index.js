const functions = require('@google-cloud/functions-framework');
const express = require('express');
const cors = require('cors');
const { MongoClient } = require('mongodb');
const bcrypt = require('bcryptjs');

const app = express();
app.use(cors());
app.use(express.json());

const MONGODB_URI = process.env.MONGODB_URI;
const DB_NAME = process.env.MONGODB_DATABASE || 'acaigestao';

let cachedClient = null;

function nextDay(dateStr) {
  const d = new Date(dateStr + 'T00:00:00.000Z');
  d.setUTCDate(d.getUTCDate() + 1);
  return d;
}

function sanitizeDoc(doc) {
  if (!doc) return doc;
  const obj = { ...doc };
  if (obj.totalVendas != null) obj.totalVendas = toNumber(obj.totalVendas);
  if (obj.totalPorFormaPagamento) {
    const m = {};
    for (const [k, v] of Object.entries(obj.totalPorFormaPagamento)) {
      m[k] = toNumber(v);
    }
    obj.totalPorFormaPagamento = m;
  }
  if (obj.data instanceof Date) obj.data = obj.data.toISOString().slice(0, 10);
  if (Array.isArray(obj.comandas)) {
    obj.comandas = obj.comandas.map((c) => ({
      ...c,
      valorTotal: c.valorTotal != null ? toNumber(c.valorTotal) : c.valorTotal,
    }));
  }
  if (Array.isArray(obj.caixas)) {
    obj.caixas = obj.caixas.map((c) => ({
      ...c,
      valorAbertura: c.valorAbertura != null ? toNumber(c.valorAbertura) : c.valorAbertura,
      valorFechamento: c.valorFechamento != null ? toNumber(c.valorFechamento) : c.valorFechamento,
    }));
  }
  return obj;
}

function toNumber(val) {
  if (val == null) return 0;
  if (typeof val === 'number') return val;
  if (typeof val.toString === 'function') return parseFloat(val.toString()) || 0;
  return 0;
}

function comandasFiltradasPorTipo(comandas, tipoFiltro) {
  if (!tipoFiltro || tipoFiltro === 'TODOS') return comandas || [];
  return (comandas || []).filter((c) => (c.tipoProduto || 'POR_PESO') === tipoFiltro);
}

function totaisFromComandas(comandas) {
  let totalVendas = 0;
  const totalPorFormaPagamento = {};
  for (const c of comandas) {
    const v = toNumber(c.valorTotal);
    totalVendas += v;
    const f = c.formaPagamento || 'OUTROS';
    totalPorFormaPagamento[f] = (totalPorFormaPagamento[f] || 0) + v;
  }
  return { totalVendas, totalPorFormaPagamento, qtdComandas: comandas.length };
}

async function getDb() {
  if (!cachedClient || !cachedClient.topology?.isConnected()) {
    cachedClient = new MongoClient(MONGODB_URI);
    await cachedClient.connect();
  }
  return cachedClient.db(DB_NAME);
}

// POST /login
app.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ message: 'Informe usuário e senha.' });
    }
    const db = await getDb();
    const user = await db.collection('app_usuarios').findOne({
      username: username.trim().toLowerCase(),
    });
    if (!user || !user.passwordHash) {
      return res.status(401).json({ message: 'Usuário ou senha inválidos.' });
    }
    const match = bcrypt.compareSync(password, user.passwordHash);
    if (!match) {
      return res.status(401).json({ message: 'Usuário ou senha inválidos.' });
    }
    res.json({ token: user.username, username: user.username });
  } catch (err) {
    console.error('login error:', err);
    res.status(500).json({ message: 'Erro interno.' });
  }
});

// GET /resumo?idLoja=X&data=YYYY-MM-DD
app.get('/resumo', async (req, res) => {
  try {
    const { idLoja, data } = req.query;
    if (!idLoja) {
      return res.status(400).json({ message: 'idLoja obrigatório.' });
    }
    const dataStr = data || new Date().toISOString().slice(0, 10);
    const docId = `${dataStr}_${idLoja.trim()}`;
    const db = await getDb();
    const doc = await db.collection('resumo_gestao').findOne({ _id: docId });
    if (!doc) {
      return res.status(404).json({ message: 'Nenhum dado encontrado.' });
    }
    res.json(sanitizeDoc(doc));
  } catch (err) {
    console.error('resumo error:', err);
    res.status(500).json({ message: 'Erro interno.' });
  }
});

// GET /lojas — lista lojas distintas na collection resumo_gestao
app.get('/lojas', async (req, res) => {
  try {
    const db = await getDb();
    const pipeline = [
      { $match: { idLoja: { $ne: null, $exists: true } } },
      { $group: { _id: '$idLoja', nomeLoja: { $first: '$nomeLoja' } } },
      { $sort: { nomeLoja: 1 } },
    ];
    const lojas = await db.collection('resumo_gestao').aggregate(pipeline).toArray();
    res.json(lojas.filter((l) => l._id).map((l) => ({ id: l._id, nome: l.nomeLoja || l._id })));
  } catch (err) {
    console.error('lojas error:', err);
    res.status(500).json({ message: 'Erro interno.' });
  }
});

// GET /resumos?idLoja=X&data=Y&dataInicio=Y&dataFim=Y
app.get('/resumos', async (req, res) => {
  try {
    const { idLoja, data, dataInicio, dataFim } = req.query;
    const filter = {};
    if (idLoja) filter.idLoja = idLoja.trim();
    if (data) {
      filter.data = { $gte: new Date(data + 'T00:00:00.000Z'), $lt: nextDay(data) };
    } else if (dataInicio || dataFim) {
      filter.data = {};
      if (dataInicio) filter.data.$gte = new Date(dataInicio + 'T00:00:00.000Z');
      if (dataFim) filter.data.$lt = nextDay(dataFim);
    }
    const db = await getDb();
    const docs = await db
      .collection('resumo_gestao')
      .find(filter)
      .sort({ data: -1 })
      .limit(500)
      .toArray();
    res.json(docs.map(sanitizeDoc));
  } catch (err) {
    console.error('resumos error:', err);
    res.status(500).json({ message: 'Erro interno.' });
  }
});

// GET /dashboard?data=YYYY-MM-DD | dataInicio=&dataFim=&tipoProduto= — agrega por loja (dia único ou período)
app.get('/dashboard', async (req, res) => {
  try {
    const tipoQ = req.query.tipoProduto;
    const tipoFiltro =
      tipoQ === 'POR_PESO' || tipoQ === 'PRECO_FIXO' ? tipoQ : null;

    const di = req.query.dataInicio;
    const df = req.query.dataFim;
    let dateFilter;
    let payloadMeta;
    if (di && df) {
      dateFilter = {
        $gte: new Date(di + 'T00:00:00.000Z'),
        $lt: nextDay(df),
      };
      payloadMeta = {
        data: di === df ? di : `${di} a ${df}`,
        dataInicio: di,
        dataFim: df,
      };
    } else {
      const dataStr = req.query.data || new Date().toISOString().slice(0, 10);
      dateFilter = {
        $gte: new Date(dataStr + 'T00:00:00.000Z'),
        $lt: nextDay(dataStr),
      };
      payloadMeta = { data: dataStr, dataInicio: dataStr, dataFim: dataStr };
    }

    const db = await getDb();
    const filter = {
      idLoja: { $ne: null, $exists: true },
      data: dateFilter,
    };
    const docs = await db.collection('resumo_gestao').find(filter).limit(3000).toArray();

    const byLoja = new Map();
    for (const d of docs) {
      const id = d.idLoja;
      let L = byLoja.get(id);
      if (!L) {
        L = {
          idLoja: id,
          nomeLoja: d.nomeLoja || id,
          totalVendas: 0,
          totalPorFormaPagamento: {},
          qtdComandas: 0,
        };
        byLoja.set(id, L);
      }
      const raw = Array.isArray(d.comandas) ? d.comandas : [];
      if (tipoFiltro) {
        const comandas = comandasFiltradasPorTipo(raw, tipoFiltro);
        const t = totaisFromComandas(comandas);
        L.totalVendas += t.totalVendas;
        L.qtdComandas += t.qtdComandas;
        for (const [k, v] of Object.entries(t.totalPorFormaPagamento)) {
          L.totalPorFormaPagamento[k] = (L.totalPorFormaPagamento[k] || 0) + v;
        }
      } else {
        // Alinhar à tela Consulta (aplicarFiltroTipoProduto): totais vêm das comandas quando existirem.
        if (raw.length > 0) {
          const t = totaisFromComandas(raw);
          L.totalVendas += t.totalVendas;
          L.qtdComandas += t.qtdComandas;
          for (const [k, v] of Object.entries(t.totalPorFormaPagamento)) {
            L.totalPorFormaPagamento[k] = (L.totalPorFormaPagamento[k] || 0) + v;
          }
        } else {
          L.totalVendas += toNumber(d.totalVendas);
          L.qtdComandas += raw.length;
          if (d.totalPorFormaPagamento) {
            for (const [k, v] of Object.entries(d.totalPorFormaPagamento)) {
              L.totalPorFormaPagamento[k] =
                (L.totalPorFormaPagamento[k] || 0) + toNumber(v);
            }
          }
        }
      }
    }

    const lojas = Array.from(byLoja.values()).sort((a, b) =>
      String(a.nomeLoja || '').localeCompare(String(b.nomeLoja || ''), 'pt'),
    );
    const totalGeral = lojas.reduce((s, l) => s + l.totalVendas, 0);
    res.json({ ...payloadMeta, totalGeral, lojas });
  } catch (err) {
    console.error('dashboard error:', err);
    res.status(500).json({ message: 'Erro interno.' });
  }
});

// Health check
app.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'acai-gestao-api' });
});

functions.http('app', app);
