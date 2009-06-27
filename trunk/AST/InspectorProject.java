package archrecovery.explorer.analysis.inspector;


import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import archrecovery.explorer.analysis.astvisitor.AnalysisASTVisitor;
import archrecovery.explorer.analysis.astvisitor.values.ProyectoSR;

public class InspectorProject {

	private AnalysisASTVisitor visitor;

	public InspectorProject(){
		visitor = new AnalysisASTVisitor();
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see net.sourceforge.earticleast.app.AbstractASTArticle#run(org.eclipse.jdt.core.ICompilationUnit)
	 */
	public void run(ICompilationUnit lwUnit) {
		CompilationUnit unit = parse(lwUnit);
		visitor.process(unit);
		if (this.visitor.getElementoActual() != null) {
			/*
			 *  Recupero las metricas que analizo el Plugin Metric's.
			 */
			//AbstractMetricSource ms = Dispatcher.getAbstractMetricSource(unit.getJavaElement());
			//this.visitor.getElementoActual().setMs(ms);
		}
		//rewrite(unit, localVariableDetector.getLocalVariableManagers());

	}

	public void run(IClassFile lwUnit) {
		CompilationUnit unit = parse(lwUnit);
		visitor.process(unit);
	}


	protected CompilationUnit parse(ICompilationUnit lwUnit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(lwUnit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}

	protected CompilationUnit parse(IClassFile lwUnit) {
		if (!lwUnit.isOpen() ) {
			try {
				lwUnit.open(null);
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(lwUnit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}

	public void process(IPackageFragmentRoot pr) throws JavaModelException{

		IJavaElement[] elements;
			elements = pr.getChildren();

			for(int i=0; i<elements.length; i++) {
				IJavaElement e = elements[i];
				if (e instanceof ICompilationUnit) {
					this.run((ICompilationUnit)e);
				} else {
					if (!(e instanceof IClassFile ))
						elements = concatElements(elements, ((IParent)e).getChildren());
					if (e instanceof IPackageFragment ) {

					}
				}
			}
	}

	public void process(IJavaProject project) {

		IJavaElement[] e;
		try {
			ProyectoSR proy = new ProyectoSR(project);
			proy.setNombre(project.getElementName());
			this.visitor.setProyecto(proy);
			e = project.getChildren();
			for (int i=0; i<e.length; i++){
				if (e[i] instanceof IPackageFragmentRoot) {
					this.process((IPackageFragmentRoot)e[i]);
				}
			}
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void process(IJavaProject project, ICompilationUnit lwUnit) {
		ProyectoSR proy = new ProyectoSR(project);
		proy.setNombre(project.getElementName());
		if (!this.visitor.isExistProyecto(proy))
			this.visitor.setProyecto(proy);
		this.run(lwUnit);
	}

	public void process(IJavaProject project, IClassFile lwUnit) {
		ProyectoSR proy = new ProyectoSR(project);
		proy.setNombre(project.getElementName());
		if (!this.visitor.isExistProyecto(proy))
			this.visitor.setProyecto(proy);
		this.run(lwUnit);
	}

	private IJavaElement[] concatElements(IJavaElement[] elements, IJavaElement[] elements2) {
		IJavaElement[] e = new IJavaElement[elements.length + elements2.length];
		int i=0;
		for (; i<elements.length; i++){
			e[i] = elements[i];
		}
		for (int j=0; j<elements2.length; j++){
			e[i] = elements2[j];
			i++;
		}
		return e;

	}
	public AnalysisASTVisitor getVisitor() {
		return this.visitor;
	}
	public void setVisitor(AnalysisASTVisitor visitor) {
		this.visitor = visitor;
	}
}
