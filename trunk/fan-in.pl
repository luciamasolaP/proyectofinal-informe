interface(paquete1_a1).
interface(paquete1_a2).

object_name(paquete1_a1,a1).
object_name(paquete1_a2,a2).

class(paquete1_b).
class(paquete1_c1).
class(paquete1_c2).

object_name(paquete1_b,b).
object_name(paquete1_c1,c1).
object_name(paquete1_c2,c2).

method(paquete1_a1_m).
method(paquete1_a2_m).
method(paquete1_b_m).
method(paquete1_c1_m).
method(paquete1_c2_m).

object_name(paquete1_a1_m,m).
object_name(paquete1_a2_m,m).
object_name(paquete1_b_m,m).
object_name(paquete1_c1_m,m).
object_name(paquete1_c2_m,m).

inherits(paquete1_c1,paquete1_b).
inherits(paquete1_c2,paquete1_b).

implements(paquete1_b,paquete1_a1).
implements(paquete1_b,paquete1_a2).

call(paquete2_x_fx,paquete1_b_m).
call(paquete2_x_fx,paquete1_c2_m).
call(paquete2_x_fx,paquete1_c1_m).
call(paquete2_x_fx,paquete1_a2_m).
call(paquete2_x_fx,paquete1_a1_m).

define-in(paquete1_a1_m,paquete1_a1).
define-in(paquete1_a2_m,paquete1_a2).
define-in(paquete1_b_m,paquete1_b).
define-in(paquete1_c1_m,paquete1_c1).
define-in(paquete1_c2_m,paquete1_c2).

% inicializa fan-in_metric de todos los metodos con 0.
init_fan-in_metric(Method):-	method(Method),								
								assert(fan-in_metric(Method,0)),
								fail.

init_fan-in_metric(Method):-	method(Method).

% calcula  el fan-in de todos lo métodos en la base.
calculate_fan-in_metric(Method):-		method(Method),			
																		remove_marks,
																		calculate(Method),																	
																		fail.

calculate_fan-in_metric(Method):-	method(Method).									
									
calculate(Method):-		fan-in_metric(Method,OldMetric),
											count_callers(Method),			% cuento la cantidad de llamados que tengo.
											fan-in_metric(Method,NewMetric),
											Metric is NewMetric - OldMetric,
											Metric > 0,												
											acum_fan-in(Method,Metric).			% calcula el fan-in acumulado.				
								
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

acum_fan-in(Method,Metric):-	object_name(Method,MethodName),
								define-in(Method,Class),			% obtiene la clase del método llamado.	
								acum_up(MethodName,Class,Metric),		% suma fan-in hacia arriba.
								acum_down(MethodName,Class,Metric).		% suma fan-in hacia abajo.								
						
acum_up(MethodName,Class,AcumMetric):-	acum_implemented_interfaces(MethodName,Class,AcumMetric),
										acum_super_classes(MethodName,Class,AcumMetric).							

acum_down(MethodName,Class,AcumMetric):-	acum_implementators(MethodName,Class,AcumMetric),		% se puede agregar que Class sea una Interface, ya que sino no puede ser implementada.
											acum_sub_classes(MethodName,Class,AcumMetric).

acum_implemented_interfaces(MethodName,Class,AcumMetric):-	interface(Interface),
															implements(Class,Interface),
															object_name(InterfaceMethod,MethodName),											
															define-in(InterfaceMethod,Interface),		% lo cuento sólo si está definido.
															not(implements_counted(Class,Interface)),		
															assert(implements_counted(Class,Interface)),														
															fan-in_metric(InterfaceMethod,Metric),
															NewMetric is Metric + AcumMetric,
															retract(fan-in_metric(InterfaceMethod,_)),
															assert(fan-in_metric(InterfaceMethod,NewMetric)),
															acum_up(MethodName,Interface,AcumMetric),
															fail.
									
acum_implemented_interfaces(_,_,_).

acum_super_classes(MethodName,Class,AcumMetric):-	class(SuperClass),
													inherits(Class,SuperClass),
													object_name(SuperClassMethod,MethodName),											
													define-in(SuperClassMethod,SuperClass),				% lo cuento sólo si está definido.
													not(inherit_counted(Class,SuperClass)),		
													assert(inherit_counted(Class,SuperClass)),														
													fan-in_metric(SuperClassMethod,Metric),
													NewMetric is Metric + AcumMetric,
													retract(fan-in_metric(SuperClassMethod,_)),
													assert(fan-in_metric(SuperClassMethod,NewMetric)),										
													acum_up(MethodName,SuperClass,AcumMetric),
													fail.
									
acum_super_classes(_,_,_).

acum_implementators(MethodName,Interface,AcumMetric):-	implements(Implementator,Interface),	
														object_name(ImplementatorMethod,MethodName),											
														define-in(ImplementatorMethod,Implementator),	
														not(implements_counted(Implementator,Interface)),		
														assert(implements_counted(Implementator,Interface)),														
														fan-in_metric(ImplementatorMethod,Metric),
														NewMetric is Metric + AcumMetric,
														retract(fan-in_metric(ImplementatorMethod,_)),
														assert(fan-in_metric(ImplementatorMethod,NewMetric)),
														acum_down(MethodName,Implementator,AcumMetric),
														fail.
									
acum_implementators(_,_,_).

acum_sub_classes(MethodName,Class,AcumMetric):-		class(SubClass),
													inherits(SubClass,Class),
													object_name(SubClassMethod,MethodName),											
													define-in(SubClassMethod,SubClass),	
													not(inherit_counted(SubClass,Class)),		
													assert(inherit_counted(SubClass,Class)),														
													fan-in_metric(SubClassMethod,Metric),
													NewMetric is Metric + AcumMetric,
													retract(fan-in_metric(SubClassMethod,_)),
													assert(fan-in_metric(SubClassMethod,NewMetric)),
													acum_down(MethodName,SubClass,AcumMetric),
													fail.
									
acum_sub_classes(_,_,_).

remove_marks:-	remove_implements_marks,
				remove_iherits_marks.

remove_implements_marks:-	retract(implements_counted(_,_)),					
							fail.

remove_implements_marks.

remove_iherits_marks:-	retract(inherit_counted(_,_)),					
						fail.

remove_iherits_marks.