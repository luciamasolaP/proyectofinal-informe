package archrecovery.explorer.analysis.astvisitor;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import archrecovery.explorer.analysis.astvisitor.filter.ExcludePatternFilter;
import archrecovery.explorer.analysis.astvisitor.values.ElementoSR;
import archrecovery.explorer.analysis.astvisitor.values.InvocacionSR;
import archrecovery.explorer.analysis.astvisitor.values.MethodSR;
import archrecovery.explorer.analysis.astvisitor.values.PaqueteSR;
import archrecovery.explorer.analysis.astvisitor.values.ProyectoSR;
import archrecovery.util.Constants;
import archrecovery.log.LogManager;

/**
 *
 * Analizador de Código Fuente.
 *
 * @author Fernandez,Mariano (fernandez.mariano.a@gmail.com)
 *
 */
public class AnalysisASTVisitor extends ASTVisitor {

	private Hashtable clases = new Hashtable();
	private Map paquetes = new Hashtable();
	private Map proyectos = new Hashtable();

	private ElementoSR elementoActual = null;
	private ProyectoSR proyecto;
	private MethodSR method = null;
	private String paqueteActual = null;

	private ExcludePatternFilter excludePattern;

	public AnalysisASTVisitor() {
		this.excludePattern = new ExcludePatternFilter();
	}

	/**
	 *  Visita un Paquete
	 */
	public boolean visit(PackageDeclaration node) {
		//System.out.println("paquete( "+node.getName() + " )");

		if (!this.excludePattern.toExclude(node.getName().getFullyQualifiedName())) {
			//this.elementoActual = new ElementoSR();
			//this.addElementoToPaquete(node.getName().getFullyQualifiedName(), this.elementoActual);
			this.paqueteActual  = node.getName().getFullyQualifiedName();
			this.elementoActual = null;
			return true;
		}
		return false;
	}

	/**
	 *  Visita un Import
	 */
	public boolean visit(ImportDeclaration node) {
		return true;
	}

	/**
	 *  Visita una Clase
	 */
	public boolean visit(TypeDeclaration node) {

		try {
		if (!this.excludePattern.toExclude(this.paqueteActual+"."+node.getName().getFullyQualifiedName())) {
			if (this.clases.get(this.proyecto.getNombre()+"."+this.paqueteActual+"."+node.getName().getFullyQualifiedName()) != null) { // lo agregue en un import
				this.elementoActual = (ElementoSR)this.clases.get(this.proyecto.getNombre()+"."+this.paqueteActual+"."+node.getName().getFullyQualifiedName());
			} else { // la primera vez que lo visito
				this.elementoActual =  this.addElementToProject(node.getName().getFullyQualifiedName(), this.paqueteActual, this.proyecto.getNombre(), this.proyecto.getJavaProject());
			}

		//this.elementoActual.setProyecto(this.getProyecto());

		if (node.isInterface()) {
			LogManager.println(this.getClass().getCanonicalName(), "interface( '" + node.getName().getFullyQualifiedName() + "' ).", LogManager.DEBUG);
			this.elementoActual.setTipoElemento(Constants.TYPE_CLASS.INTERFACE);
		} else {
			List lism = node.modifiers();
			Iterator i = lism.iterator();
			boolean clase_abstract = false;
			while (i.hasNext()) {
				IExtendedModifier ie = (IExtendedModifier)i.next();
				if (ie.isModifier()) { //determina si la clase es abstracta.
					if (((Modifier)ie).isAbstract()) {
						clase_abstract = true;
						LogManager.println(this.getClass().getCanonicalName(), "abstract( '" + node.getName().getFullyQualifiedName() + "' ).", LogManager.DEBUG);
						this.elementoActual.setTipoElemento(Constants.TYPE_CLASS.ABSTRACT);
					}
				}
			}
			if (!clase_abstract) {
				LogManager.println(this.getClass().getCanonicalName(), "clase( '" + node.getName().getFullyQualifiedName() + "' ).", LogManager.DEBUG);
				this.elementoActual.setTipoElemento(Constants.TYPE_CLASS.CLASS);
			}
		}

		// determina de que quien extiende.
		Type type = node.getSuperclassType();
		if ((type != null) && type.isSimpleType()){
			LogManager.println(this.getClass().getCanonicalName(), "extends( '"+ node.getName().getFullyQualifiedName() + "', '" + ((SimpleType)type).getName()+ "' ).", LogManager.DEBUG);
			String paquete = "";
			if (type.resolveBinding() != null)
				paquete = type.resolveBinding().getPackage().getName(); //busco el paquete al que pertenece.

			this.elementoActual.setRextends(paquete+"."+((SimpleType)type).getName().getFullyQualifiedName());
		}

		// determina de que interfaces implementa.
		List lis = node.superInterfaceTypes();
		if (lis != null) {
			Iterator ite = lis.iterator();
			while (ite.hasNext()) {
				Type typ = (Type)ite.next();
				if (typ.isSimpleType()) {
					System.out.println("interface( '" + ((SimpleType)typ).getName() + "' ).");
					String paquete = "";
					if (typ.resolveBinding() != null)
						paquete = typ.resolveBinding().getPackage().getName(); //busco el paquete al que pertenece.
					//String proyecto = typ.resolveBinding().getJavaElement().getJavaProject().getElementName(); //Nombre del proyecto.

						if (node.isInterface()) { // Interface extends Interface
							System.out.println("extends( '"+ node.getName().getFullyQualifiedName() + "', '" + ((SimpleType)typ).getName() + "' ).");
							this.elementoActual.setRextends(this.elementoActual.getRextends()+"|"+paquete+"."+((SimpleType)typ).getName().getFullyQualifiedName());
						} else { // Clase o Abstract implements Interface
							System.out.println("implements( '"+ node.getName().getFullyQualifiedName() + "', '" + ((SimpleType)typ).getName() + "' ).");
							if (this.elementoActual.getRimplements().equals(""))
								this.elementoActual.setRimplements(paquete+"."+((SimpleType)typ).getName().getFullyQualifiedName());
							else
								this.elementoActual.setRimplements(this.elementoActual.getRimplements()+"|"+paquete+"."+((SimpleType)typ).getName().getFullyQualifiedName());
						}
				}
			}
		}

		this.clases.put(this.elementoActual.getPaquete().getPaquete()+"."+this.elementoActual.getNombre(), this.elementoActual);
		return true;
		}
		} catch (Exception e ) {
			System.out.println(e);
		}

		return false;
	}




	/**
	 * Starts the process.
	 *
	 * @param unit
	 *            the AST root node. Bindings have to have been resolved.
	 */
	public void process(CompilationUnit unit) {
		unit.accept(this);
	}

	/**
	 * Retorna todas las clases analizadas.
	 *
	 * @return List<ElementoSR>
	 */
	public List<ElementoSR> getClases(){
		Iterator ite = this.clases.keySet().iterator();
		List<ElementoSR> le = new ArrayList<ElementoSR>();
		while (ite.hasNext()) {
			ElementoSR e = (ElementoSR)this.clases.get(ite.next());
			le.add(e);
		}
		return le;
	}

	/**
	 * Inicializa el Analizador.
	 *
	 */
	public void reset(){
		this.clases = new Hashtable();
		this.paquetes = new Hashtable();
		this.proyectos = new Hashtable();
	}

	/**
	 *  Retorna el proyecto que se esta analizando.
	 *
	 * @return ProyectoSR.
	 */
	public ProyectoSR getProyecto() {
		return proyecto;
	}

	/**
	 * Set un proyecto.
	 *
	 * @param proyecto
	 */
	public void setProyecto(ProyectoSR proyecto) {
		this.proyecto = proyecto;
		this.proyectos.put(this.proyecto.getNombre(), this.proyecto);
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		/*List lisv = node.fragments();
		Iterator ite = lisv.iterator();
		while(ite.hasNext()) {
			VariableDeclarationFragment v = (VariableDeclarationFragment)ite.next();

			System.out.println("Atributo : "+v.getName());
			System.out.println("Tipo del Atributo : "+v.resolveBinding().getDeclaringClass().getQualifiedName());
		}
*/
		return true;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		LogManager.println(this.getClass().getName(),"Metodo: visit(MethodInvocation node)" , LogManager.DEBUG);

		if (node.resolveMethodBinding() != null ) {
			String nobj = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
			String nm = node.getName().getFullyQualifiedName();
			int cp = node.arguments().size();

			LogManager.println(this.getClass().getName(),
								" INVOCACION: "+node.resolveMethodBinding().getDeclaringClass().getQualifiedName()+
								"-->"+node.getName() +
								"("+node.arguments().size()+")"
								, LogManager.DEBUG);

			if (!this.excludePattern.toExclude(nobj)) { // veo que con  quien me relaciono sea de interes.

					try {
					String n = node.resolveMethodBinding().getDeclaringClass().getName();
					String p = node.resolveMethodBinding().getDeclaringClass().getPackage().getName();
					String proy = node.resolveMethodBinding().getJavaElement().getJavaProject().getElementName();
					String treturn = node.resolveMethodBinding().getMethodDeclaration().getReturnType().getQualifiedName();

					ITypeBinding[] lis = node.resolveMethodBinding().getMethodDeclaration().getParameterTypes();
					String parameters = "";
					if (lis.length>0) {
						for ( ITypeBinding tp: lis){
							parameters += tp.getQualifiedName()+ "|";
						}
						parameters = parameters.substring(0, parameters.length()-1);
					}

					InvocacionSR inv = new InvocacionSR();
					inv.setClaseObjecto(p+"."+n);
					inv.setMetodo(node.getName().getFullyQualifiedName());
					inv.setParametrosFormales(parameters);
					inv.setTreturn(treturn);

					this.method.addInvocacion(inv);
					//getInvocacion().put(nobj + "_" + nm + "_" + cp, inv);
					} catch (Exception e) {
						LogManager.println(this.getClass().getName(), "ERROR", LogManager.ERROR, e);
					}

				return true;
			}
		}
		return false;
	}

	@Override
	//Declaracion de variables locales.
	public boolean visit(VariableDeclarationExpression node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	public ElementoSR getElementoActual() {
		return elementoActual;
	}

	public void setElementoActual(ElementoSR elementoActual) {
		this.elementoActual = elementoActual;
	}

	/**
	 * Visita la declaracion de un metodo.
	 */
	public boolean visit(MethodDeclaration node) {
		LogManager.println(this.getClass().getName(),"Metodo: visit(MethodDeclaration node)" , LogManager.DEBUG);
		LogManager.println(this.getClass().getName(),"Nombre del Metodo: "+node.getName()+" Cant. de Parametros : "+node.parameters().size() , LogManager.DEBUG);

		String typesParameters = "";
		String treturn = "";

		if (node.resolveBinding()!=null) { //trato de resolver el binding
			ITypeBinding[] imethodbin = node.resolveBinding().getParameterTypes(); //recupero el binding con los parametros

			for (ITypeBinding p: imethodbin){
					typesParameters += p.getQualifiedName() + "|";
			}
			if (imethodbin.length>0)
				typesParameters = typesParameters.substring(0, typesParameters.length()-1);
			if (!node.isConstructor()) {
				ITypeBinding ireturn = node.resolveBinding().getReturnType();
				treturn = ireturn.getQualifiedName();
			}
		} else {
			if (!node.isConstructor()) {
				treturn = node.getReturnType2().toString();
			}
		}

		MethodSR method = null;
		method = new MethodSR(this.elementoActual);
		method.setNombre(node.getName().getFullyQualifiedName());
		method.setCparametros(new Integer(node.parameters().size()));
		method.setTypeParameters(typesParameters);
		method.setTypeReturn(treturn);

		if (this.elementoActual.getMethod(method.getFullNombre()) == null ) {
			this.elementoActual.putMethod(method);
		} else {
			method = (MethodSR)this.elementoActual.getMethod(node.getName()+"-"+typesParameters);
		}
		this.method = method;

		return true;
	}

	/**
	 *  Agrega un elemento dentro de su paquete.
	 *
	 * @param paquete : Paquete donde incorporar al Elemento.
	 * @param e : Elemento
	 * @param proyecto : Proyecto al que corresponde el Elemento.
	 */
	private void addElementoToPaquete(String paquete, ElementoSR e, ProyectoSR proyecto) {
		PaqueteSR pSR = null;
		if (this.paquetes.get(proyecto.getNombre()+"."+paquete) == null ){
			pSR = new PaqueteSR();
			pSR.setPaquete(paquete);
			pSR.setProyecto(proyecto);
			proyecto.getPaquetes().add(pSR);
			this.paquetes.put(proyecto.getNombre()+"."+paquete, pSR);
		} else {
			pSR = (PaqueteSR)this.paquetes.get(proyecto.getNombre()+"."+paquete);
		}

		e.setPaquete(pSR);
		pSR.addElementoSR(e);
	}

	/**
	 * Agrega un Elemento a dentro de su Proyecto.
	 *
	 * @param enombre : Nombre del Elemento.
	 * @param epaquete : Nombre del Paquete.
	 * @param eproyecto : Nombre del Proyecto.
	 * @param javaProject : Referencia al proyecto Java de donde proviene.
	 *
	 * @return ElementoSR.
	 */
	private ElementoSR addElementToProject(String enombre, String epaquete, String eproyecto, IJavaProject javaProject){
		ProyectoSR p = null;
		if (this.proyectos.get(eproyecto) == null) {
			p = new ProyectoSR(javaProject);
			p.setNombre(eproyecto);
			this.proyectos.put(eproyecto, p);
		}

		p = (ProyectoSR)this.proyectos.get(eproyecto);


		ElementoSR elSR = null;
		if (this.clases.get(eproyecto+"."+epaquete+"."+enombre) == null){
			elSR = new ElementoSR();
			elSR.setNombre(enombre);
			this.addElementoToPaquete(epaquete, elSR, p);

			elSR.setTipoElemento(Constants.TYPE_CLASS.DESC); // puede ser una clase o una clase abstracta hasta no ver su definicion no se
			this.clases.put(eproyecto+"."+epaquete+"."+enombre, elSR);
		} else {
			elSR = (ElementoSR)this.clases.get(eproyecto+"."+epaquete+"."+enombre);
		}

		return elSR;
	}

	/**
	 * Retorna todos los paquetes del proyecto actual.
	 *
	 * @return List<PaqueteSR>.
	 */
	public List<PaqueteSR> getPaquetes() {
		Iterator ite = this.paquetes.keySet().iterator();
		List<PaqueteSR> lp = new ArrayList<PaqueteSR>();
		while (ite.hasNext()) {
			PaqueteSR e = (PaqueteSR)this.paquetes.get((String)ite.next());
			lp.add(e);
		}
		return lp;
	}

	/**
	 * Retorna todos los Proyectos que se han analizados.
	 *
	 * @return List<ProyectoSR>.
	 */
	public List<ProyectoSR> getProyectos() {
		Iterator ite = this.proyectos.keySet().iterator();
		List<ProyectoSR> lp = new ArrayList<ProyectoSR>();
		while (ite.hasNext()) {
			ProyectoSR p = (ProyectoSR)this.proyectos.get((String)ite.next());
			lp.add(p);
		}
		return lp;
	}

	/**
	 * Determina si un dado proyecto existe.
	 *
	 * @param proyecto : Proyecto
	 *
	 * @return boolean.
	 */
	public boolean isExistProyecto(ProyectoSR proyecto){
		return this.proyectos.get(proyecto.getNombre()) == null ? false : true;
	}

}