import {
  zenumlHighlighter,
  zenumlLinter,
  zenumlCompletions,
  zenumlCompletionKeyMaps,
  zenumlCompletionListener,
} from '@zenuml/codemirror-extensions';
import {
  bracketMatching,
  foldGutter,
  HighlightStyle,
  IndentContext,
  indentService,
  syntaxHighlighting,
} from '@codemirror/language';
import {
  mermaid,
  mindmapTags,
  flowchartTags,
  ganttTags,
  sequenceTags,
  pieTags,
  requirementTags,
  journeyTags,
  mermaidTags,
} from 'codemirror-lang-mermaid';
import { closeBrackets, acceptCompletion, autocompletion } from '@codemirror/autocomplete';
import { defaultKeymap, history, indentWithTab, undo, redo } from '@codemirror/commands';
import { EditorState, EditorSelection } from '@codemirror/state';
import { lineNumbers, placeholder, keymap, EditorView } from '@codemirror/view';
import { dracula } from 'thememirror';

const mermaidHighlightStyle = HighlightStyle.define([
  { tag: mermaidTags.diagramName, color: '#81c784', fontWeight: 'bold' },
  { tag: flowchartTags.keyword, color: '#2cabf5' },
  { tag: flowchartTags.orientation, color: '#2cabf5' },
  { tag: flowchartTags.lineComment, color: '#6272a4', fontStyle: 'italic' },
  { tag: flowchartTags.link, color: '#ff79c6' },
  { tag: flowchartTags.nodeEdge, color: '#ff79c6' },
  { tag: flowchartTags.nodeEdgeText, color: '#74f67a' },
  { tag: flowchartTags.nodeId, color: '#fff' },

  { tag: ganttTags.keyword, color: '#2cabf5' },
  { tag: ganttTags.lineComment, color: '#6272a4', fontStyle: 'italic' },
  { tag: ganttTags.string, color: '#74f67a' },

  { tag: sequenceTags.arrow, color: '#ff79c6' },
  { tag: sequenceTags.keyword1, color: '#ff79c6' },
  { tag: sequenceTags.keyword2, color: '#ff79c6' },
  { tag: sequenceTags.lineComment, color: '#6272a4', fontStyle: 'italic' },
  { tag: sequenceTags.messageText1, color: '#fff' },
  { tag: sequenceTags.messageText2, color: '#fff' },
  { tag: sequenceTags.nodeText, color: '#fff' },
  { tag: sequenceTags.position, color: '#fff' },

  { tag: pieTags.lineComment, color: '#6272a4', fontStyle: 'italic' },
  { tag: pieTags.number, color: '#2cabf5' },
  { tag: pieTags.showData, color: '#2cabf5' },
  { tag: pieTags.string, color: '#74f67a' },
  { tag: pieTags.title, color: '#74f67a' },
  { tag: pieTags.titleText, color: '#74f67a' },

  { tag: mindmapTags.lineText1, color: '#ce9178' },
  { tag: mindmapTags.lineText2, color: '#74f67a' },
  { tag: mindmapTags.lineText3, color: '#e1bee7' },
  { tag: mindmapTags.lineText4, color: '#2cabf5' },
  { tag: mindmapTags.lineText5, color: '#ff79c6' },

  { tag: requirementTags.arrow, color: '#ff79c6' },
  { tag: requirementTags.keyword, color: '#2cabf5' },
  { tag: requirementTags.lineComment, color: '#6272a4', fontStyle: 'italic' },
  { tag: requirementTags.number, color: '#2cabf5' },
  { tag: requirementTags.quotedString, color: '#81c784' },
  { tag: requirementTags.unquotedString, color: '#74f67a' },

  { tag: journeyTags.actor, color: '#ff79c6' },
  { tag: journeyTags.keyword, color: '#2cabf5' },
  { tag: journeyTags.lineComment, color: '#6272a4', fontStyle: 'italic' },
  { tag: journeyTags.score, color: '#2cabf5' },
  { tag: journeyTags.text, color: '#74f67a' },
]);

function customIndent(context: IndentContext, pos: number) {
  let line = context.lineAt(pos);
  let prevLine = pos > 0 ? context.lineAt(pos - 1) : null;

  // arrow pattern
  const arrowPattern = /^[a-z0-9]+->[a-z0-9]+([.:]\w+)?$/i;

  // method definition pattern
  const methodPattern = /^[a-z0-9.]+\w+\s*{$/i;

  // Check if the current line is inside braces
  if (prevLine) {
    const prevLineText = prevLine.text.trim();
    if (prevLineText.endsWith("{")) {
      return context.lineIndent(prevLine.from) + context.unit;
    }
  }

  // Check if the current line matches the arrow pattern
  if (arrowPattern.test(line.text.trim())) {
    return context.lineIndent(line.from);
  }

  // Check if the previous line matches the arrow pattern
  if (prevLine && arrowPattern.test(prevLine.text.trim())) {
    return context.lineIndent(prevLine.from);
  }

  // Check if the previous line is a method definition
  if (prevLine && methodPattern.test(prevLine.text.trim())) {
    return context.lineIndent(prevLine.from) + context.unit;
  }


  return null;
}

const customIndentExtension = indentService.of((context, pos) => customIndent(context, pos));

const baseExtensionsFactory = (onEditorCodeChange: (code: string) => void) => [
  dracula,
  closeBrackets(),
  lineNumbers(),
  foldGutter(),
  bracketMatching(),
  history(),
  placeholder('Write you code here'),
  EditorState.tabSize.of(2),
  customIndentExtension,
  keymap.of([
    ...defaultKeymap,
    { key: 'Tab', run: acceptCompletion },
    { key: 'Enter', run: acceptCompletion },
    indentWithTab,
    { key: "Mod-z", run: undo, preventDefault: true },
    { key: "Mod-Shift-z", run: redo, preventDefault: true },
  ]),
  EditorView.lineWrapping,
  EditorView.updateListener.of((update) => {
    if (update.docChanged) {
      const updatedCode = update.state.doc.toString();
      onEditorCodeChange(updatedCode)
    }
  }),
];

const insertMermaidComment = (view: EditorView): boolean => {
  view.dispatch(view.state.changeByRange(range => ({
    changes: { from: range.from, insert: "%%  %% " },
    range: EditorSelection.cursor(range.from + 3)
  })));
  return true;
};

const mermaidExtensions = [
  mermaid(),
  syntaxHighlighting(mermaidHighlightStyle),
  keymap.of([
    { key: 'Mod-/', run: insertMermaidComment, preventDefault: true },
  ]),
]

const zenumlExtensions = [
  zenumlHighlighter('dark'),
  zenumlLinter(),
  autocompletion({
    override: [zenumlCompletions],
    activateOnTyping: true,
    closeOnBlur: true,
    icons: true,
    selectOnOpen: true,
  }),
  keymap.of(zenumlCompletionKeyMaps),
  EditorView.updateListener.of(zenumlCompletionListener)
]

export { baseExtensionsFactory, mermaidExtensions, zenumlExtensions };
