interface(a1).
interface(a2).

class(b).
class(c1).
class(c2).

method(m).

inherits(c1,b).
inherits(c2,b).

implements(b,a1).
implements(b,a2).

% De donde se invoca el método m.
call(mB,m).
%call(mA1,m).
%call(mA2,m).
%call(mC1,m).
%call(mC2,m).

define-in(mB,b).
define-in(mA1,a1).
define-in(mA2,a2).
define-in(mC1,c1).
define-in(mC2,c2).
define-in(m,j).
define-in(m,a1).

% inicializa fan-in_metric de todos los metodos con 0.
init_fan-in_metric(Method):-	method(Method),								
								assert(fan-in_metric(Method,0)),
								fail.

init_fan-in_metric(Method):-	method(Method).

% calcula  el fan-in de todos lo métodos en la base.
calculate_fan-in_metric(Method):-	method(Method),
									count_callers(Method),			% cuento la cantidad de llamados que tengo.
									acum_fan-in(Method),			% calcula el fan-in acumulado.								
									fail.

calculate_fan-in_metric(Method):-	method(Method).

% cuenta los llamados directos a un método.
count_callers(Method):-		fan-in_metric(Method,Metric),	
							count_callers(Method,Metric).									

count_callers(Method,Metric):-		call(Caller,Method),
									not(call_counted(Caller,Method)),		
									assert(call_counted(Caller,Method)),	
									NewMetric is Metric + 1,
									retract(fan-in_metric(Method,_)),
									assert(fan-in_metric(Method,NewMetric)),
									count_callers(Method,NewMetric).	
							
count_callers(_,_).		

acum_fan-in(Method):-	call(Caller,Method),
						class(Class),
						define-in(Caller,Class),	% obtiene la clase del método llamador.		aca no tendría que obtener la clase del método llamado?
						count_up(Method,Class),		% suma fan-in hacia arriba.
						count_down(Method,Class).	% suma fan-in hacia abajo.
						
count_up(Method,Class):-	count_implemented_interfaces(Method,Class),
							count_super_classes(Method,Class).							

count_down(Method,Class):-	count_implementators(Method,Class),		% se puede agregar que Class sea una Interface, ya que sino no puede ser implementada.
							count_sub_classes(Method,Class).

% suma tantas unidades al fan-in del método como hijos tenga.  aca no sería sumar al método que pertence al hijo el fan in tantas veces como lo tenga el padre? lo mismo para el resto
count_sub_classes(Method,Class):-	class(SubClass),
									inherits(SubClass,Class),
									not(inherit_counted(SubClass,Class)),		
									assert(inherit_counted(SubClass,Class)),														
									fan-in_metric(Method,Metric),
									NewMetric is Metric + 1,
									retract(fan-in_metric(Method,_)),
									assert(fan-in_metric(Method,NewMetric)),
									count_down(Method,SubClass),
									fail.
									
count_sub_classes(_,_).

% suma tantas unidades al fan-in del método como padres tenga.
count_super_classes(Method,Class):-		class(SuperClass),
										inherits(Class,SuperClass),
										define-in(Method,SuperClass),		% lo cuento sólo si está definido.
										not(inherit_counted(Class,SuperClass)),		
										assert(inherit_counted(Class,SuperClass)),														
										fan-in_metric(Method,Metric),
										NewMetric is Metric + 1,
										retract(fan-in_metric(Method,_)),
										assert(fan-in_metric(Method,NewMetric)),										
										count_up(Method,SuperClass),
										fail.
									
count_super_classes(_,_).

% suma tantas unidades al fan-in del método como interfaces implementa.
count_implemented_interfaces(Method,Class):-	interface(Interface),
												implements(Class,Interface),
												define-in(Method,Interface),		% lo cuento sólo si está definido.
												not(implements_counted(Class,Interface)),		
												assert(implements_counted(Class,Interface)),														
												fan-in_metric(Method,Metric),
												NewMetric is Metric + 1,
												retract(fan-in_metric(Method,_)),
												assert(fan-in_metric(Method,NewMetric)),
												count_up(Interface),
												fail.
									
count_implemented_interfaces(_,_).

% suma tantas unidades al fan-in del método como clases me implementen.
count_implementators(Method,Interface):-	implements(Implementator,Interface),	% se podría agegar que Implementator debe ser Class o Interface.
											not(implements_counted(Implementator,Interface)),		
											assert(implements_counted(Implementator,Interface)),														
											fan-in_metric(Method,Metric),
											NewMetric is Metric + 1,
											retract(fan-in_metric(Method,_)),
											assert(fan-in_metric(Method,NewMetric)),
											count_down(Implementator),
											fail.
									
count_implementators(_,_).
				
