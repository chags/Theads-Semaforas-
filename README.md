# Brincadeira de Crianças — Semáforos

**Instituto Federal de Educação, Ciência e Tecnologia do Ceará — IFCE**

| | |
|---|---|
| **Disciplina** | Sistemas Operacionais |
| **Professor** | Parente |
| **Aluno** | Francisco Cristiano Chagas |

---

Simulação do problema clássico de sincronização entre threads usando semáforos em Java.

Este trabalho implementa um cenário onde **N crianças** compartilham um **cesto de bolas com capacidade limitada K**, utilizando semáforos para coordenar o acesso concorrente sem deadlocks nem condições de corrida. Cada criança é representada por uma thread independente que alterna entre brincar, devolver a bola, descansar e pegar uma nova bola — demonstrando na prática os conceitos de exclusão mútua, sincronização e comunicação entre processos estudados na disciplina de Sistemas Operacionais. A aplicação conta com interface gráfica animada em Java Swing, onde cada estado da criança é exibido por um sprite animado distinto e acompanhado de efeito sonoro.

## Descrição do Problema

N crianças compartilham um cesto com capacidade para K bolas.

- M crianças começam **com** bola; as demais começam **sem** bola.
- Quem tem bola **brinca** pelo tempo Tb e depois **devolve** ao cesto.
- Quem não tem bola **aguarda** uma bola no cesto e então **brinca**.
- Se o cesto estiver **cheio**, a criança segura a bola até abrir espaço.
- Se o cesto estiver **vazio**, a criança espera até alguém colocar uma bola.
- O ciclo se repete eternamente: brinca → devolve → descansa → pega → brinca…

## Sincronização

O cesto é controlado por dois semáforos em `BallBasket.java`:

| Semáforo     | Valor inicial | Bloqueia quem…                          |
|--------------|---------------|-----------------------------------------|
| `emptySlots` | K             | quer **colocar** bola (cesto cheio)     |
| `balls`      | 0             | quer **pegar** bola (cesto vazio)       |

**Colocar bola:**
```
emptySlots.acquire()  → conta.incrementa()  → balls.release()
```

**Pegar bola:**
```
balls.acquire()  → conta.decrementa()  → emptySlots.release()
```

## Estrutura do Projeto

```
theads/
├── src/brincadeira/
│   ├── Main.java             # Ponto de entrada; inicia a janela na EDT
│   ├── MainFrame.java        # Interface gráfica (Swing)
│   ├── BallBasket.java       # Cesto — lógica dos semáforos
│   ├── Child.java            # Thread criança — ciclo de vida completo
│   ├── ChildState.java       # Enum com os quatro estados possíveis
│   └── ChildTableModel.java  # Modelo da tabela de crianças
├── compile.sh
├── run.sh
└── README.md
```

## Como Executar

**Pré-requisito:** Java 11 ou superior.

```bash
# Compilar (necessário na primeira vez ou após alterações no código)
bash compile.sh

# Executar
bash run.sh
```

Ou em um único comando:

```bash
bash compile.sh && bash run.sh
```

## Interface

| Elemento            | Descrição                                              |
|---------------------|--------------------------------------------------------|
| Spinner **K**       | Capacidade do cesto (fixada ao criar a primeira criança) |
| **+ Adicionar Criança** | Abre diálogo para configurar ID, Tb, Td e bola inicial |
| Tabela              | Status em tempo real de cada criança (colorido por estado) |
| Log de Eventos      | Histórico com timestamp de cada ação                   |

### Cores da Tabela

| Cor     | Estado                        |
|---------|-------------------------------|
| Verde   | Brincando                     |
| Amarelo | Aguardando bola no cesto      |
| Laranja | Aguardando espaço no cesto    |
| Azul    | Descansando                   |

## Parâmetros de Cada Criança

| Parâmetro | Descrição                                  |
|-----------|--------------------------------------------|
| **ID**    | Identificador (texto livre)                |
| **Bola?** | Se a criança inicia com ou sem bola        |
| **Tb**    | Tempo de brincadeira em milissegundos      |
| **Td**    | Tempo de descanso em milissegundos         |

## Detalhes de Implementação

- **Busy wait:** os tempos Tb e Td usam espera ativa (`Thread.onSpinWait()`) — a thread permanece no estado RUNNABLE, sem bloquear em semáforo nem chamar `sleep`.
- **Prioridade mínima:** cada thread criança roda com `Thread.MIN_PRIORITY` para não estrangular a Event Dispatch Thread do Swing.
- **Encerramento:** fechar a janela chama `System.exit(0)`, encerrando todas as threads daemon imediatamente.
